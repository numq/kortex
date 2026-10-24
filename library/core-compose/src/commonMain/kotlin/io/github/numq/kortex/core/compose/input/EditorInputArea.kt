package io.github.numq.kortex.core.compose.input

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.input.*
import io.github.numq.kortex.core.EditorEngine
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.compose.dimensions.EditorDimensions
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.key.EditorKeyContext
import io.github.numq.kortex.core.compose.keymap.DefaultEditorKeymap
import io.github.numq.kortex.core.compose.keymap.EditorKeymap
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.edit.EditAction
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.selection.SelectionAction
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.text.TextRange as ComposeTextRange

private const val IME_SENTINEL = "\u200B"
private const val MULTILINE_SELECTION_PLACEHOLDER = "\uFFFC"

internal class EditorTextInputBridge(
    private val engine: EditorEngine,
    private val isReadOnlyProvider: () -> Boolean,
) {
    var session: TextInputSession? = null
        private set

    var composingRange: TextRange? = null
        private set

    private var currentFieldValue: TextFieldValue = TextFieldValue(
        text = IME_SENTINEL,
        selection = ComposeTextRange(1, 1),
    )

    fun startSession(textInputService: TextInputService) {
        stopSession()

        val initialState = engine.state.value
        composingRange = null
        currentFieldValue = buildFieldValueFromState(initialState)

        session = textInputService.startInput(
            value = currentFieldValue,
            imeOptions = ImeOptions(
                singleLine = false,
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.None,
            ),
            onEditCommand = ::onEditCommands,
            onImeActionPerformed = ::onImeAction,
        )
    }

    fun stopSession() {
        composingRange = null
        session?.dispose()
        session = null
    }

    fun clearComposition() {
        composingRange = null
    }

    fun syncWithEditorState(state: EditorState) {
        val activeSession = session ?: return

        val primary = state.primarySelection
        val caretLine = primary.caret.line
        val currentComp = composingRange

        if (currentComp != null && (currentComp.start.line != caretLine || currentComp.end.line != caretLine)) {
            composingRange = null
        }

        val previousValue = currentFieldValue
        val nextValue = buildFieldValueFromState(state)

        if (previousValue == nextValue) return

        currentFieldValue = nextValue
        activeSession.updateState(oldValue = previousValue, newValue = nextValue)
    }

    fun updateFocusedRect(rect: Rect) {
        session?.notifyFocusedRect(rect)
    }

    private fun onImeAction(imeAction: ImeAction) {
        if (isReadOnlyProvider()) return

        when (imeAction) {
            ImeAction.Default, ImeAction.None, ImeAction.Done, ImeAction.Go, ImeAction.Next, ImeAction.Send, ImeAction.Search -> {
                composingRange = null
                engine.dispatch(EditAction.Enter)
            }

            else -> Unit
        }
    }

    private fun onEditCommands(commands: List<EditCommand>) {
        if (commands.isEmpty() || isReadOnlyProvider()) return

        for (command in commands) {
            val state = engine.state.value

            when (command) {
                is CommitTextCommand -> {
                    val activeComp = composingRange
                    when {
                        activeComp != null -> {
                            engine.applyOperation(
                                TextOperation.Data.Single.Replace(activeComp, command.text),
                                canMerge = false,
                            )
                            composingRange = null
                        }

                        command.text == "\n" -> engine.dispatch(EditAction.Enter)
                        command.text == "\t" -> engine.dispatch(EditAction.Tab)
                        else -> engine.dispatch(EditAction.Insert(command.text))
                    }
                }

                is DeleteSurroundingTextCommand -> {
                    val activeComp = composingRange
                    when {
                        activeComp != null -> {
                            engine.applyOperation(TextOperation.Data.Single.Delete(activeComp), canMerge = false)
                            composingRange = null
                        }

                        state.primarySelection.isNotEmpty -> {
                            engine.dispatch(EditAction.Backspace)
                        }

                        command.lengthBeforeCursor == 1 && command.lengthAfterCursor == 0 -> {
                            engine.dispatch(EditAction.Backspace)
                        }

                        command.lengthBeforeCursor > 0 || command.lengthAfterCursor > 0 -> {
                            val caret = state.primarySelection.caret
                            val lineText = state.snapshot.getLineText(caret.line)
                            val startCol = (caret.column - command.lengthBeforeCursor).coerceAtLeast(0)
                            val endCol = (caret.column + command.lengthAfterCursor).coerceAtMost(lineText.length)
                            if (startCol < endCol) {
                                engine.applyOperation(
                                    TextOperation.Data.Single.Delete(
                                        TextRange(TextPosition(caret.line, startCol), TextPosition(caret.line, endCol))
                                    ),
                                    canMerge = false,
                                )
                            } else {
                                engine.dispatch(EditAction.Backspace)
                            }
                        }
                    }
                }

                is SetComposingTextCommand -> {
                    val activeComp = composingRange
                    val startPos = activeComp?.start ?: state.primarySelection.range.start
                    val text = command.text

                    if (activeComp != null) {
                        engine.applyOperation(
                            TextOperation.Data.Single.Replace(activeComp, text),
                            canMerge = false,
                        )
                    } else if (state.primarySelection.isNotEmpty) {
                        engine.applyOperation(
                            TextOperation.Data.Single.Replace(state.primarySelection.range, text),
                            canMerge = false,
                        )
                    } else {
                        engine.applyOperation(
                            TextOperation.Data.Single.Insert(startPos, text),
                            canMerge = false,
                        )
                    }

                    composingRange = if (text.isNotEmpty() && !text.contains('\n')) {
                        TextRange(startPos, TextPosition(startPos.line, startPos.column + text.length))
                    } else {
                        null
                    }
                }

                is SetComposingRegionCommand -> {
                    val line = state.primarySelection.caret.line
                    val lineLen = state.snapshot.getLineLength(line)
                    val s = (command.start - 1).coerceIn(0, lineLen)
                    val e = (command.end - 1).coerceIn(s, lineLen)
                    composingRange = if (s < e) TextRange(TextPosition(line, s), TextPosition(line, e)) else null
                }

                is FinishComposingTextCommand -> {
                    composingRange = null
                }

                is BackspaceCommand, is DeleteSurroundingTextInCodePointsCommand -> {
                    val activeComp = composingRange
                    if (activeComp != null) {
                        engine.applyOperation(
                            TextOperation.Data.Single.Delete(activeComp),
                            canMerge = false,
                        )
                        composingRange = null
                    } else {
                        engine.dispatch(EditAction.Backspace)
                    }
                }

                is DeleteAllCommand -> {
                    composingRange = null
                    engine.dispatch(SelectionAction.SelectAll)
                    engine.dispatch(EditAction.Backspace)
                }

                is MoveCursorCommand -> {
                    val pos = state.primarySelection.caret
                    val lineLen = state.snapshot.getLineLength(pos.line)
                    val targetCol = (pos.column + command.amount).coerceIn(0, lineLen)
                    engine.dispatch(CaretAction.Move(TextPosition(pos.line, targetCol), withSelection = false))
                }

                is SetSelectionCommand -> {
                    val line = state.primarySelection.caret.line
                    val lineLen = state.snapshot.getLineLength(line)
                    val s = (command.start - 1).coerceIn(0, lineLen)
                    val e = (command.end - 1).coerceIn(s, lineLen)
                    engine.dispatch(
                        SelectionAction.SetSelections(
                            listOf(Selection.fromPositions(TextPosition(line, s), TextPosition(line, e)))
                        )
                    )
                }
            }
        }
    }

    private fun buildFieldValueFromState(state: EditorState): TextFieldValue {
        val primary = state.primarySelection
        val snapshot = state.snapshot

        if (!primary.range.isSingleLine) {
            val start = primary.range.start
            val end = primary.range.end
            val startLineText = snapshot.getLineText(start.line)
            val endLineText = snapshot.getLineText(end.line)
            val beforeText = startLineText.take(start.column.coerceIn(0, startLineText.length))
            val afterText = endLineText.drop(end.column.coerceIn(0, endLineText.length))
            val prefix = IME_SENTINEL + beforeText
            val text = prefix + MULTILINE_SELECTION_PLACEHOLDER + afterText
            return TextFieldValue(
                text = text,
                selection = ComposeTextRange(prefix.length, prefix.length + MULTILINE_SELECTION_PLACEHOLDER.length),
                composition = null,
            )
        }

        val line = primary.caret.line.coerceIn(0, (snapshot.lines - 1).coerceAtLeast(0))
        val lineText = snapshot.getLineText(line)

        val text = IME_SENTINEL + lineText
        val selStart = primary.anchor.column.coerceIn(0, lineText.length) + IME_SENTINEL.length
        val selEnd = primary.caret.column.coerceIn(0, lineText.length) + IME_SENTINEL.length

        val composition =
            composingRange?.takeIf { it.start.line == line && it.end.line == line && !it.isEmpty }?.let { range ->
                val cs = range.start.column.coerceIn(0, lineText.length) + IME_SENTINEL.length
                val ce = range.end.column.coerceIn(0, lineText.length) + IME_SENTINEL.length
                if (cs < ce) ComposeTextRange(cs, ce) else null
            }

        return TextFieldValue(
            text = text,
            selection = ComposeTextRange(selStart, selEnd),
            composition = composition,
        )
    }
}

@Composable
fun EditorInputArea(
    modifier: Modifier = Modifier,
    engine: EditorEngine,
    font: EditorFont,
    scrollController: EditorScrollController,
    gutterWidth: Float,
    dimensions: EditorDimensions,
    readOnly: Boolean,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onContextMenu: (Offset, TextPosition) -> Unit,
    onDismissContextMenu: () -> Unit = {},
    handleCopy: () -> Unit,
    handleCut: () -> Unit,
    handlePaste: () -> Unit,
    keymap: EditorKeymap = remember { DefaultEditorKeymap.create() },
    onGutterClick: ((line: Int, offset: Offset) -> Boolean)? = null,
    content: @Composable () -> Unit,
) {
    val state by engine.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val textInputService = LocalTextInputService.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val viewConfiguration = LocalViewConfiguration.current

    val currentGutterWidth by rememberUpdatedState(gutterWidth)
    val currentFont by rememberUpdatedState(font)
    val currentDimensions by rememberUpdatedState(dimensions)
    val currentReadOnly by rememberUpdatedState(readOnly)
    val currentKeymap by rememberUpdatedState(keymap)
    val currentOnGutterClick by rememberUpdatedState(onGutterClick)
    val currentOnContextMenu by rememberUpdatedState(onContextMenu)
    val currentOnDismissContextMenu by rememberUpdatedState(onDismissContextMenu)

    var isFocused by remember { mutableStateOf(false) }

    val imeBridge = remember(engine) {
        EditorTextInputBridge(
            engine = engine,
            isReadOnlyProvider = { currentReadOnly },
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(isFocused, readOnly, textInputService) {
        if (isFocused && !readOnly && textInputService != null) {
            imeBridge.startSession(textInputService)
        } else {
            imeBridge.stopSession()
        }
    }

    DisposableEffect(imeBridge) {
        onDispose {
            imeBridge.stopSession()
        }
    }

    LaunchedEffect(state) {
        imeBridge.syncWithEditorState(state)
    }

    LaunchedEffect(
        isFocused,
        state.primarySelection.caret,
        state.foldMap,
        scrollController.horizontalOffset,
        scrollController.verticalOffset,
        gutterWidth,
        font,
        dimensions,
    ) {
        if (!isFocused) return@LaunchedEffect

        val caret = state.primarySelection.caret
        val visualLine = state.foldMap.documentToVisualLine(caret.line)
        val snappedLineHeight = ceil(font.lineHeight)
        val lineText = state.snapshot.getLineText(caret.line)
        val safeCol = caret.column.coerceIn(0, lineText.length)
        val xOffset = font.measureTextWidth(lineText.take(safeCol))

        val left = gutterWidth + dimensions.editorPaddingStart + xOffset - scrollController.horizontalOffset
        val top = (visualLine * snappedLineHeight) - scrollController.verticalOffset
        val rect = Rect(
            left = left,
            top = top,
            right = left + dimensions.caretWidth,
            bottom = top + snappedLineHeight,
        )
        imeBridge.updateFocusedRect(rect)
    }

    val keyContext = remember(engine, scrollController, readOnly) {
        object : EditorKeyContext {
            override val engine: EditorEngine = engine
            override val scrollController: EditorScrollController = scrollController
            override val isReadOnly: Boolean = readOnly
            override fun copy() = handleCopy()
            override fun cut() = handleCut()
            override fun paste() = handlePaste()
        }
    }

    fun calculatePositionAtOffset(offset: Offset): TextPosition {
        val currentState = engine.state.value
        val snapshot = currentState.snapshot
        val foldMap = currentState.foldMap

        val effectiveScrollY = maxOf(0f, scrollController.verticalOffset)
        val snappedLineHeight = ceil(currentFont.lineHeight).coerceAtLeast(1f)
        val visualLineIndex = floor((offset.y + effectiveScrollY) / snappedLineHeight).toInt()

        val totalVisualLines = foldMap.totalVisualLines
        val safeVisualLine = visualLineIndex.coerceIn(0, maxOf(0, totalVisualLines - 1))
        val documentLine = foldMap.visualToDocumentLine(safeVisualLine)

        val localX = maxOf(
            0f,
            offset.x - currentGutterWidth + scrollController.horizontalOffset - currentDimensions.editorPaddingStart,
        )

        val lineText = snapshot.getLineText(documentLine)
        val column = currentFont.getOffsetAtCoordinate(lineText, localX)
        val lineLength = snapshot.getLineLength(documentLine)

        return TextPosition(line = documentLine, column = column.coerceIn(0, lineLength))
    }

    fun calculateCaretOffset(position: TextPosition): Offset {
        val currentState = engine.state.value
        val snapshot = currentState.snapshot
        val foldMap = currentState.foldMap

        val safeLine = position.line.coerceIn(0, (snapshot.lines - 1).coerceAtLeast(0))
        val visualLine = foldMap.documentToVisualLine(safeLine)
        val snappedLineHeight = ceil(currentFont.lineHeight).coerceAtLeast(1f)
        val lineText = snapshot.getLineText(safeLine)
        val safeCol = position.column.coerceIn(0, lineText.length)
        val xOffset = currentFont.measureTextWidth(lineText.take(safeCol))

        val left =
            currentGutterWidth + currentDimensions.editorPaddingStart + xOffset - scrollController.horizontalOffset
        val top = (visualLine * snappedLineHeight) - scrollController.verticalOffset
        return Offset(
            x = left.coerceAtLeast(currentGutterWidth),
            y = top.coerceAtLeast(0f),
        )
    }

    fun performEdgeAutoScroll(pointerPosition: Offset) {
        val viewportHeight = scrollController.viewportHeight
        val viewportWidth = scrollController.viewportWidth
        val scrollZone = currentFont.lineHeight * 1.5f

        val autoScrollDeltaY = when {
            pointerPosition.y < scrollZone -> -currentFont.lineHeight * ((scrollZone - pointerPosition.y) / scrollZone).coerceIn(
                0.5f, 3f
            )

            viewportHeight > 0f && pointerPosition.y > viewportHeight - scrollZone -> currentFont.lineHeight * ((pointerPosition.y - (viewportHeight - scrollZone)) / scrollZone).coerceIn(
                0.5f, 3f
            )

            else -> 0f
        }

        val leftEdge = currentGutterWidth + scrollZone
        val autoScrollDeltaX = when {
            pointerPosition.x > currentGutterWidth && pointerPosition.x < leftEdge -> -currentFont.charWidth * 2f
            viewportWidth > 0f && pointerPosition.x > viewportWidth - scrollZone -> currentFont.charWidth * 2f
            else -> 0f
        }

        if (autoScrollDeltaX != 0f || autoScrollDeltaY != 0f) {
            scrollController.scrollBy(deltaX = autoScrollDeltaX, deltaY = autoScrollDeltaY)
        }
    }

    fun dispatchTapSelection(
        tapCount: Int,
        textPos: TextPosition,
        isShift: Boolean,
        isAlt: Boolean,
    ): Selection? {
        return when {
            isAlt -> {
                val newSelection = Selection.cursor(textPos)
                engine.dispatch(SelectionAction.AddSelection(newSelection))
                newSelection
            }

            tapCount == 2 -> {
                engine.dispatch(SelectionAction.SelectWordAt(textPos))
                null
            }

            tapCount >= 3 -> {
                engine.dispatch(SelectionAction.SelectLine(textPos.line))
                null
            }

            else -> {
                engine.dispatch(CaretAction.Move(textPos, withSelection = isShift))
                null
            }
        }
    }

    val pointerInputModifier = Modifier.pointerInput(engine, viewConfiguration) {
        var lastTapTimeMillis = 0L
        var lastTapOffset = Offset.Zero
        var tapCount = 0
        var flingJob: Job? = null

        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull() ?: continue

                if (event.type == PointerEventType.Scroll) {
                    currentOnDismissContextMenu()
                    flingJob?.cancel()

                    var totalDeltaX = 0f
                    var totalDeltaY = 0f

                    for (c in event.changes) {
                        totalDeltaX += c.scrollDelta.x
                        totalDeltaY += c.scrollDelta.y
                    }

                    val scrollMultiplier = currentFont.lineHeight * 2.5f
                    val isShiftPressed = event.keyboardModifiers.isShiftPressed

                    val effectiveDeltaX = if (isShiftPressed && totalDeltaX == 0f) totalDeltaY else totalDeltaX
                    val effectiveDeltaY = if (isShiftPressed) 0f else totalDeltaY

                    scrollController.scrollBy(
                        deltaX = effectiveDeltaX * scrollMultiplier,
                        deltaY = effectiveDeltaY * scrollMultiplier,
                    )

                    event.changes.forEach { it.consume() }
                    continue
                }

                if (event.type != PointerEventType.Press) continue

                flingJob?.cancel()

                val isMouse = change.type == PointerType.Mouse

                if (isMouse && event.buttons.isSecondaryPressed) {
                    focusRequester.requestFocus()
                    val textPos = calculatePositionAtOffset(change.position)
                    val currentSelection = engine.state.value.primarySelection
                    if (currentSelection.isEmpty || !currentSelection.range.contains(textPos)) {
                        engine.dispatch(CaretAction.Move(textPos, withSelection = false))
                    }
                    val menuOffset = calculateCaretOffset(textPos)
                    currentOnContextMenu(menuOffset, textPos)
                    change.consume()
                    continue
                }

                if (isMouse && !event.buttons.isPrimaryPressed) {
                    continue
                }

                currentOnDismissContextMenu()

                val downOffset = change.position
                val downTimeMillis = change.uptimeMillis
                val isWithinMultiTapWindow =
                    (downTimeMillis - lastTapTimeMillis) <= viewConfiguration.doubleTapTimeoutMillis && (downOffset - lastTapOffset).getDistance() <= viewConfiguration.touchSlop * 2f

                tapCount = if (isWithinMultiTapWindow) (tapCount % 3) + 1 else 1
                lastTapTimeMillis = downTimeMillis
                lastTapOffset = downOffset

                if (isMouse) {
                    focusRequester.requestFocus()

                    if (downOffset.x < currentGutterWidth) {
                        val pos = calculatePositionAtOffset(downOffset)
                        val handled = currentOnGutterClick?.invoke(pos.line, downOffset) ?: false
                        if (!handled) {
                            engine.dispatch(SelectionAction.SelectLine(pos.line))
                        }
                        change.consume()
                        continue
                    }

                    val isAlt = event.keyboardModifiers.isAltPressed
                    val isShift = event.keyboardModifiers.isShiftPressed
                    val textPos = calculatePositionAtOffset(downOffset)
                    val currentDragSelection = dispatchTapSelection(tapCount, textPos, isShift, isAlt)

                    change.consume()

                    while (true) {
                        val dragEvent = awaitPointerEvent(PointerEventPass.Main)
                        val dragChange = dragEvent.changes.firstOrNull() ?: break

                        if (dragEvent.type == PointerEventType.Release || !dragChange.pressed) {
                            dragChange.consume()
                            break
                        }

                        if (dragEvent.type == PointerEventType.Move) {
                            performEdgeAutoScroll(dragChange.position)

                            val dragPos = calculatePositionAtOffset(dragChange.position)
                            when {
                                isAlt && currentDragSelection != null -> {
                                    val updatedSelection = Selection.fromPositions(
                                        anchor = textPos,
                                        caret = dragPos,
                                        stickyColumn = dragPos.column,
                                    )
                                    val currentState = engine.state.value
                                    val withoutLast = currentState.selections.dropLast(1)
                                    engine.dispatch(SelectionAction.SetSelections(withoutLast + updatedSelection))
                                }

                                else -> engine.dispatch(CaretAction.Move(dragPos, withSelection = true))
                            }

                            dragChange.consume()
                        }
                    }
                } else {
                    change.consume()

                    val touchSlop = viewConfiguration.touchSlop
                    val longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis
                    val velocityTracker = VelocityTracker().apply {
                        addPosition(change.uptimeMillis, downOffset)
                    }

                    var isPanning = false
                    var isLongPressConfirmed = false
                    var isReleased = false
                    var latestOffset = downOffset

                    val timeoutResult = withTimeoutOrNull(longPressTimeoutMillis) {
                        while (true) {
                            val nextEvent = awaitPointerEvent(PointerEventPass.Main)
                            val nextChange = nextEvent.changes.firstOrNull()

                            if (nextChange == null || nextEvent.type == PointerEventType.Release || !nextChange.pressed) {
                                nextChange?.consume()
                                isReleased = true
                                break
                            }

                            if (nextEvent.type == PointerEventType.Move) {
                                latestOffset = nextChange.position
                                velocityTracker.addPosition(nextChange.uptimeMillis, latestOffset)
                                val distance = (latestOffset - downOffset).getDistance()
                                if (distance > touchSlop) {
                                    isPanning = true
                                    val panDelta = nextChange.positionChange()
                                    scrollController.scrollBy(deltaX = -panDelta.x, deltaY = -panDelta.y)
                                    nextChange.consume()
                                    break
                                }
                            }
                        }
                    }

                    if (timeoutResult == null && !isReleased && !isPanning) {
                        isLongPressConfirmed = true
                    }

                    when {
                        isReleased -> {
                            focusRequester.requestFocus()

                            if (downOffset.x < currentGutterWidth) {
                                val pos = calculatePositionAtOffset(downOffset)
                                val handled = currentOnGutterClick?.invoke(pos.line, downOffset) ?: false
                                if (!handled) {
                                    engine.dispatch(SelectionAction.SelectLine(pos.line))
                                }
                            } else {
                                val textPos = calculatePositionAtOffset(downOffset)

                                if (tapCount == 1 && !currentReadOnly) {
                                    imeBridge.session?.showSoftwareKeyboard()
                                    softwareKeyboardController?.show()
                                }

                                dispatchTapSelection(
                                    tapCount = tapCount,
                                    textPos = textPos,
                                    isShift = false,
                                    isAlt = false,
                                )
                            }
                        }

                        isPanning -> {
                            currentOnDismissContextMenu()
                            while (true) {
                                val panEvent = awaitPointerEvent(PointerEventPass.Main)
                                val panChange = panEvent.changes.firstOrNull() ?: break

                                if (panEvent.type == PointerEventType.Release || !panChange.pressed) {
                                    velocityTracker.addPosition(panChange.uptimeMillis, panChange.position)
                                    panChange.consume()
                                    break
                                }

                                if (panEvent.type == PointerEventType.Move) {
                                    velocityTracker.addPosition(panChange.uptimeMillis, panChange.position)
                                    val delta = panChange.positionChange()
                                    if (delta != Offset.Zero) {
                                        scrollController.scrollBy(deltaX = -delta.x, deltaY = -delta.y)
                                    }
                                    panChange.consume()
                                }
                            }

                            val velocity = velocityTracker.calculateVelocity()
                            var vx = -velocity.x
                            var vy = -velocity.y

                            if (abs(vx) > 50f || abs(vy) > 50f) {
                                flingJob = coroutineScope.launch {
                                    val frameStepSeconds = 0.016f
                                    val friction = 0.92f
                                    while (isActive && (abs(vx) > 15f || abs(vy) > 15f)) {
                                        scrollController.scrollBy(
                                            deltaX = vx * frameStepSeconds,
                                            deltaY = vy * frameStepSeconds,
                                        )
                                        vx *= friction
                                        vy *= friction
                                        delay(16L.milliseconds)
                                    }
                                }
                            }
                        }

                        isLongPressConfirmed -> {
                            if (downOffset.x < currentGutterWidth) {
                                val pos = calculatePositionAtOffset(downOffset)
                                val handled = currentOnGutterClick?.invoke(pos.line, downOffset) ?: false
                                if (!handled) {
                                    engine.dispatch(SelectionAction.SelectLine(pos.line))
                                }
                            } else {
                                focusRequester.requestFocus()

                                if (!currentReadOnly) {
                                    imeBridge.session?.showSoftwareKeyboard()
                                    softwareKeyboardController?.show()
                                }

                                val textPos = calculatePositionAtOffset(downOffset)
                                engine.dispatch(CaretAction.Move(textPos, withSelection = false))

                                var releaseOffset = latestOffset
                                while (true) {
                                    val dragEvent = awaitPointerEvent(PointerEventPass.Main)
                                    val dragChange = dragEvent.changes.firstOrNull() ?: break

                                    if (dragEvent.type == PointerEventType.Release || !dragChange.pressed) {
                                        releaseOffset = dragChange.position
                                        dragChange.consume()
                                        break
                                    }

                                    if (dragEvent.type == PointerEventType.Move) {
                                        releaseOffset = dragChange.position
                                        if ((releaseOffset - downOffset).getDistance() > touchSlop) {
                                            performEdgeAutoScroll(releaseOffset)
                                            val dragPos = calculatePositionAtOffset(releaseOffset)
                                            engine.dispatch(CaretAction.Move(dragPos, withSelection = false))
                                        }
                                        dragChange.consume()
                                    }
                                }

                                val finalPos = calculatePositionAtOffset(releaseOffset)
                                engine.dispatch(CaretAction.Move(finalPos, withSelection = false))

                                val caretMenuOffset = calculateCaretOffset(finalPos)
                                currentOnContextMenu(caretMenuOffset, finalPos)
                            }
                        }
                    }
                }
            }
        }
    }

    val keyboardModifier = Modifier.onPreviewKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

        if (keyEvent.isModifierKey) return@onPreviewKeyEvent false

        currentOnDismissContextMenu()

        if (currentKeymap.handle(keyEvent, keyContext)) {
            imeBridge.clearComposition()
            return@onPreviewKeyEvent true
        }

        if (keyEvent.isCommandModifierPressed) {
            return@onPreviewKeyEvent false
        }

        val codePoint = keyEvent.utf16CodePoint

        if (isPrintableCodePoint(codePoint) && !currentReadOnly) {
            imeBridge.clearComposition()
            engine.dispatch(EditAction.Insert(codePointToString(codePoint)))
            return@onPreviewKeyEvent true
        }

        false
    }

    Box(modifier = modifier.focusRequester(focusRequester).onFocusChanged { focusState ->
        isFocused = focusState.isFocused
        if (!focusState.isFocused) {
            currentOnDismissContextMenu()
        }
        onFocusChanged(focusState.isFocused)
    }.focusable().then(keyboardModifier).then(pointerInputModifier).pointerHoverIcon(PointerIcon.Text)) {
        content()
    }
}

private val KeyEvent.isModifierKey: Boolean
    get() = when (key) {
        Key.CtrlLeft, Key.CtrlRight, Key.ShiftLeft, Key.ShiftRight, Key.AltLeft, Key.AltRight, Key.MetaLeft, Key.MetaRight, Key.CapsLock, Key.ScrollLock, Key.NumLock, Key.Function -> true

        else -> false
    }

private fun isPrintableCodePoint(codePoint: Int): Boolean {
    if (codePoint <= 0 || codePoint == 0xFFFF) return false

    if (codePoint <= 0xFFFF) {
        val char = codePoint.toChar()

        return !char.isISOControl() && when (char.category) {
            CharCategory.UNASSIGNED, CharCategory.PRIVATE_USE, CharCategory.SURROGATE, CharCategory.FORMAT -> false

            else -> true
        }
    }

    return codePoint in 0x10000..0x10FFFF
}

private fun codePointToString(codePoint: Int) = when {
    codePoint <= 0xFFFF -> codePoint.toChar().toString()

    else -> {
        val high = ((codePoint - 0x10000) ushr 10) + 0xD800
        val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00

        "${high.toChar()}${low.toChar()}"
    }
}