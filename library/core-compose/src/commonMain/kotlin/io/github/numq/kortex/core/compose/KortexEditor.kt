package io.github.numq.kortex.core.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import io.github.numq.kortex.core.EditorEngine
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.compose.canvas.EditorCanvas
import io.github.numq.kortex.core.compose.dimensions.EditorDimensions
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.input.EditorInputArea
import io.github.numq.kortex.core.compose.keymap.DefaultEditorKeymap
import io.github.numq.kortex.core.compose.keymap.EditorKeymap
import io.github.numq.kortex.core.compose.layer.LayerFactory
import io.github.numq.kortex.core.compose.layer.rememberLayerFactory
import io.github.numq.kortex.core.compose.layout.EditorLayoutInfo
import io.github.numq.kortex.core.compose.menu.DefaultEditorMenu
import io.github.numq.kortex.core.compose.menu.EditorMenuData
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.compose.scroll.rememberEditorScrollController
import io.github.numq.kortex.core.compose.theme.EditorTheme
import io.github.numq.kortex.core.edit.EditAction
import io.github.numq.kortex.core.history.HistoryAction
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.selection.SelectionAction
import io.github.numq.kortex.core.viewport.Viewport
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange

/**
 * Hardware-accelerated Compose Multiplatform text editor component rendered via Skia Canvas.
 *
 * Handles viewport virtualization, multi-cursor rendering, IME text input, pointer gestures,
 * auto-scrolling, and keyboard shortcuts.
 *
 * @param engine The [EditorEngine] driving the document state and history.
 * @param font The [EditorFont] used for text measurement and Skia paragraph rendering.
 * @param theme Visual color theme for the editor surface, gutter, selection, and carets.
 * @param modifier The [Modifier] to be applied to the root editor container.
 * @param showGutter Whether to render the line number gutter on the left side.
 * @param readOnly Whether text editing mutations and cut/paste actions are disabled.
 * @param dimensions Layout padding and sizing metrics for the editor and gutter.
 * @param scrollController Controller managing horizontal and vertical viewport scroll offsets.
 * @param bottomInset Additional bottom inset (in pixels) to account for overlaid panels.
 * @param keymap Keyboard shortcut bindings active within the editor.
 * @param layerFactory Factory responsible for creating cached Skia rendering layers.
 * @param onGutterClick Optional callback invoked when a line in the gutter is clicked; return `true` if consumed.
 * @param canvas Slot for rendering the underlying editor canvas.
 * @param overlay Composable slot rendered above the canvas (e.g., for popups or search bars).
 * @param menu Composable slot for the right-click / long-press context menu, or `null` to disable.
 */
@Composable
fun KortexEditor(
    engine: EditorEngine,
    font: EditorFont,
    theme: EditorTheme,
    modifier: Modifier = Modifier,
    showGutter: Boolean = true,
    readOnly: Boolean = false,
    dimensions: EditorDimensions = remember { EditorDimensions() },
    scrollController: EditorScrollController = rememberEditorScrollController(),
    bottomInset: Float = 0f,
    keymap: EditorKeymap = remember { DefaultEditorKeymap.create() },
    layerFactory: LayerFactory = rememberLayerFactory(dimensions = dimensions),
    onGutterClick: ((line: Int, offset: Offset) -> Boolean)? = null,
    canvas: @Composable (state: EditorState, viewport: Viewport, isFocused: Boolean, gutterWidth: Float) -> Unit = { state, viewport, isFocused, gutterWidth ->
        EditorCanvas(
            state = state,
            font = font,
            theme = theme,
            scrollController = scrollController,
            viewport = viewport,
            layerFactory = layerFactory,
            gutterWidth = gutterWidth,
            isFocused = isFocused,
            modifier = Modifier.fillMaxSize(),
        )
    },
    overlay: @Composable () -> Unit = {},
    menu: (@Composable (EditorMenuData) -> Unit)? = { data ->
        DefaultEditorMenu(data = data)
    },
) {
    val state by engine.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var menuData by remember { mutableStateOf<EditorMenuData?>(null) }

    LaunchedEffect(scrollController.horizontalOffset, scrollController.verticalOffset) {
        if (menuData != null) {
            menuData = null
        }
    }

    val gutterWidth by remember(showGutter, state.snapshot.lines, font.lineHeight, dimensions) {
        derivedStateOf {
            if (!showGutter) return@derivedStateOf 0f

            val maxTextWidth = font.measureTextWidth("${state.snapshot.lines + 1}")
            dimensions.gutterPaddingStart + maxTextWidth + dimensions.gutterGap + font.lineHeight + dimensions.gutterPaddingEnd
        }
    }

    val textContentWidth by remember(state.snapshot.maxLineLength, font.charWidth, dimensions) {
        derivedStateOf {
            dimensions.editorPaddingStart + (state.snapshot.maxLineLength * font.charWidth) + dimensions.editorPaddingEnd
        }
    }

    val contentWidth by remember(gutterWidth, textContentWidth) {
        derivedStateOf { gutterWidth + textContentWidth }
    }

    val contentHeight by remember(state.foldMap.totalVisualLines, font.lineHeight, dimensions, bottomInset) {
        derivedStateOf {
            (state.foldMap.totalVisualLines * font.lineHeight) + dimensions.contentPaddingBottom + bottomInset
        }
    }

    LaunchedEffect(state.primarySelection.caret, font, state.foldMap, dimensions, gutterWidth, bottomInset) {
        val visualLine = state.foldMap.documentToVisualLine(state.primarySelection.caret.line)

        scrollController.scrollToCaret(
            target = state.primarySelection.caret.copy(line = visualLine),
            charWidth = font.charWidth,
            lineHeight = font.lineHeight,
            gutterWidth = gutterWidth,
            editorPaddingStart = dimensions.editorPaddingStart,
            bottomInset = bottomInset,
        )
    }

    fun handleCopy() {
        val activeSelections = state.selections.filter(Selection::isNotEmpty)

        when {
            activeSelections.isNotEmpty() -> {
                val text = activeSelections.joinToString("\n") { selection ->
                    state.snapshot.getTextInRange(selection.range)
                }
                clipboardManager.setText(AnnotatedString(text))
            }

            else -> {
                val linesToCopy = state.selections.map { it.caret.line }.distinct().sorted()

                if (linesToCopy.isNotEmpty()) {
                    val delimiter = state.config.lineSeparator
                    val text = linesToCopy.joinToString(delimiter) { line ->
                        state.snapshot.getLineText(line)
                    } + delimiter
                    clipboardManager.setText(AnnotatedString(text))
                }
            }
        }
    }

    fun handleCut() {
        if (readOnly) return

        val activeSelections = state.selections.filter(Selection::isNotEmpty)

        when {
            activeSelections.isNotEmpty() -> {
                handleCopy()
                engine.dispatch(EditAction.Backspace)
            }

            else -> {
                handleCopy()
                val linesToCut = state.selections.map { it.caret.line }.distinct().sortedDescending()

                val operations = linesToCut.map { line ->
                    val lineLength = state.snapshot.getLineLength(line)
                    val isLastLine = line == state.snapshot.lines - 1

                    when {
                        isLastLine && line > 0 -> {
                            val prevLineLen = state.snapshot.getLineLength(line - 1)
                            TextOperation.Data.Single.Delete(
                                range = TextRange(
                                    start = TextPosition(line - 1, prevLineLen), end = TextPosition(line, lineLength)
                                )
                            )
                        }

                        state.snapshot.lines > 1 -> TextOperation.Data.Single.Delete(
                            range = TextRange(
                                start = TextPosition(line, 0), end = TextPosition(line + 1, 0)
                            )
                        )

                        else -> TextOperation.Data.Single.Delete(
                            range = TextRange(
                                start = TextPosition(0, 0), end = TextPosition(0, lineLength)
                            )
                        )
                    }
                }

                if (operations.isNotEmpty()) {
                    val batch = when (operations.size) {
                        1 -> operations[0]
                        else -> TextOperation.Data.Batch(operations)
                    }
                    engine.applyOperation(batch, canMerge = false)
                }
            }
        }
    }

    fun handlePaste() {
        if (readOnly) return

        val textToPaste = clipboardManager.getText()?.text ?: return
        engine.dispatch(EditAction.Paste(textToPaste))
    }

    BoxWithConstraints(modifier = modifier.background(theme.background)) {
        val viewportWidth = when {
            constraints.hasBoundedWidth -> constraints.maxWidth.toFloat()
            else -> contentWidth
        }

        val viewportHeight = when {
            constraints.hasBoundedHeight -> constraints.maxHeight.toFloat()
            else -> contentHeight
        }

        val viewport = remember(
            state.snapshot, state.foldMap, scrollController.verticalOffset, viewportWidth, viewportHeight, font
        ) {
            Viewport.of(
                snapshot = state.snapshot,
                foldMap = state.foldMap,
                width = viewportWidth,
                height = viewportHeight,
                scrollY = scrollController.verticalOffset,
                ascent = font.ascent,
                textHeight = font.textHeight,
                lineHeight = font.lineHeight,
            )
        }

        SideEffect {
            scrollController.updateLayoutInfo(
                EditorLayoutInfo(
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                    isGutterVisible = showGutter,
                    gutterWidth = gutterWidth,
                    textContentWidth = textContentWidth,
                    contentHeight = contentHeight,
                    scrollX = scrollController.horizontalOffset,
                    scrollY = scrollController.verticalOffset,
                    lineHeight = font.lineHeight,
                    charWidth = font.charWidth,
                    visibleLinesRange = viewport.visibleLines,
                )
            )
        }

        EditorInputArea(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            font = font,
            scrollController = scrollController,
            gutterWidth = gutterWidth,
            dimensions = dimensions,
            readOnly = readOnly,
            focusRequester = focusRequester,
            onFocusChanged = { isFocused = it },
            onContextMenu = { clickOffset, textPos ->
                val currentSelection = engine.state.value.primarySelection
                val isInsideSelection = currentSelection.isNotEmpty && currentSelection.range.contains(textPos)
                if (!isInsideSelection) {
                    engine.dispatch(CaretAction.Move(textPos, withSelection = false))
                }

                val activeRange = if (isInsideSelection) currentSelection.range else TextRange(textPos, textPos)
                val activeText =
                    if (isInsideSelection) engine.state.value.snapshot.getTextInRange(currentSelection.range) else ""

                menuData = EditorMenuData(
                    x = clickOffset.x,
                    y = clickOffset.y,
                    position = textPos,
                    selectedRange = activeRange,
                    selectedText = activeText,
                    isReadOnly = readOnly,
                    canUndo = engine.history.canUndo,
                    canRedo = engine.history.canRedo,
                    cut = { handleCut(); menuData = null },
                    copy = { handleCopy(); menuData = null },
                    paste = { handlePaste(); menuData = null },
                    undo = { engine.dispatch(HistoryAction.Undo); menuData = null },
                    redo = { engine.dispatch(HistoryAction.Redo); menuData = null },
                    selectAll = { engine.dispatch(SelectionAction.SelectAll); menuData = null },
                    dismiss = { menuData = null },
                )
            },
            onDismissContextMenu = { menuData = null },
            handleCopy = ::handleCopy,
            handleCut = ::handleCut,
            handlePaste = ::handlePaste,
            keymap = keymap,
            onGutterClick = onGutterClick,
        ) {
            canvas(state, viewport, isFocused, gutterWidth)
            overlay()
            menuData?.let { data -> menu?.invoke(data) }
        }
    }
}