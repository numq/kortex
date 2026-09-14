package io.github.numq.kortex.core.compose.input

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import io.github.numq.kortex.core.EditorEngine
import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.compose.dimensions.EditorDimensions
import io.github.numq.kortex.core.compose.key.EditorKeyContext
import io.github.numq.kortex.core.compose.keymap.DefaultEditorKeymap
import io.github.numq.kortex.core.compose.keymap.EditorKeymap
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.edit.EditAction
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.selection.SelectionAction
import io.github.numq.krope.text.TextPosition
import kotlin.math.floor

@Composable
fun EditorInputArea(
    engine: EditorEngine,
    scrollController: EditorScrollController,
    fontCharWidth: Float,
    fontLineHeight: Float,
    gutterWidth: Float,
    dimensions: EditorDimensions,
    readOnly: Boolean,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onContextMenu: (Offset, TextPosition) -> Unit,
    handleCopy: () -> Unit,
    handleCut: () -> Unit,
    handlePaste: () -> Unit,
    keymap: EditorKeymap = remember { DefaultEditorKeymap.create() },
    onGutterClick: ((line: Int, offset: Offset) -> Boolean)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentGutterWidth by rememberUpdatedState(gutterWidth)

    val currentCharWidth by rememberUpdatedState(fontCharWidth)

    val currentLineHeight by rememberUpdatedState(fontLineHeight)

    val currentDimensions by rememberUpdatedState(dimensions)

    val currentOnGutterClick by rememberUpdatedState(onGutterClick)

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

        val snappedLineHeight = currentLineHeight

        val visualLineIndex = floor((offset.y + effectiveScrollY) / snappedLineHeight).toInt()

        val totalVisualLines = foldMap.totalVisualLines

        val safeVisualLine = visualLineIndex.coerceIn(0, maxOf(0, totalVisualLines - 1))

        val documentLine = foldMap.visualToDocumentLine(safeVisualLine)

        val localX =
            offset.x - currentGutterWidth + scrollController.horizontalOffset - currentDimensions.editorPaddingStart

        val lineLength = snapshot.getLineLength(documentLine)

        val column = when {
            currentCharWidth > 0f -> (localX / currentCharWidth).toInt().coerceIn(0, lineLength)

            else -> 0
        }

        return TextPosition(line = documentLine, column = column)
    }

    val pointerInputModifier = Modifier.pointerInput(engine) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)

                val change = event.changes.firstOrNull() ?: continue

                if (event.type == PointerEventType.Scroll) {
                    var totalDeltaX = 0f

                    var totalDeltaY = 0f

                    for (c in event.changes) {
                        totalDeltaX += c.scrollDelta.x

                        totalDeltaY += c.scrollDelta.y
                    }

                    val scrollMultiplier = currentLineHeight * 2.5f

                    val isShiftPressed = event.keyboardModifiers.isShiftPressed

                    val effectiveDeltaX = if (isShiftPressed && totalDeltaX == 0f) totalDeltaY else totalDeltaX

                    val effectiveDeltaY = if (isShiftPressed) 0f else totalDeltaY

                    scrollController.scrollBy(
                        deltaX = effectiveDeltaX * scrollMultiplier, deltaY = effectiveDeltaY * scrollMultiplier
                    )

                    event.changes.forEach { it.consume() }

                    continue
                }

                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    val textPos = calculatePositionAtOffset(change.position)

                    onContextMenu(change.position, textPos)

                    change.consume()
                    continue
                }

                if (event.type == PointerEventType.Press && event.buttons.isPrimaryPressed) {
                    focusRequester.requestFocus()

                    val offset = change.position

                    if (offset.x < currentGutterWidth) {
                        val pos = calculatePositionAtOffset(offset)

                        val handled = currentOnGutterClick?.invoke(pos.line, offset) ?: false

                        if (!handled) {
                            engine.dispatch(SelectionAction.SelectLine(pos.line))
                        }

                        change.consume()

                        continue
                    }

                    val isAlt = event.keyboardModifiers.isAltPressed

                    val isShift = event.keyboardModifiers.isShiftPressed

                    val textPos = calculatePositionAtOffset(offset)

                    var currentDragSelection: Selection? = null

                    when {
                        isAlt -> {
                            val newSelection = Selection.cursor(textPos)

                            engine.dispatch(SelectionAction.AddSelection(newSelection))

                            currentDragSelection = newSelection
                        }

                        else -> engine.dispatch(CaretAction.Move(textPos, withSelection = isShift))
                    }

                    change.consume()

                    while (true) {
                        val dragEvent = awaitPointerEvent(PointerEventPass.Main)

                        val dragChange = dragEvent.changes.firstOrNull() ?: break

                        if (dragEvent.type == PointerEventType.Release) {
                            dragChange.consume()

                            break
                        }

                        if (dragEvent.type == PointerEventType.Move) {
                            val dragPos = calculatePositionAtOffset(dragChange.position)

                            when {
                                isAlt && currentDragSelection != null -> {
                                    val updatedSelection = Selection.fromPositions(
                                        anchor = textPos, caret = dragPos, stickyColumn = dragPos.column
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
                }
            }
        }
    }

    val keyboardModifier = Modifier.onKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

        if (keyEvent.isModifierKey) return@onKeyEvent false

        if (keymap.handle(keyEvent, keyContext)) {
            return@onKeyEvent true
        }

        if (keyEvent.isPrimaryModifierPressed) {
            return@onKeyEvent false
        }

        val codePoint = keyEvent.utf16CodePoint

        if (isPrintableCodePoint(codePoint) && !readOnly) {
            engine.dispatch(EditAction.Insert(codePointToString(codePoint)))

            return@onKeyEvent true
        }

        false
    }

    Box(
        modifier = modifier.focusRequester(focusRequester).onFocusChanged { onFocusChanged(it.isFocused) }.focusable()
            .then(keyboardModifier).then(pointerInputModifier).pointerHoverIcon(PointerIcon.Text)
    ) {
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