package io.github.numq.kortex.core.compose

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
    keymap: EditorKeymap = remember { DefaultEditorKeymap.create() },
    layerFactory: LayerFactory = rememberLayerFactory(dimensions = dimensions),
    onGutterClick: ((line: Int, offset: Offset) -> Boolean)? = null,
    canvas: @Composable (state: EditorState, viewport: Viewport, isFocused: Boolean, gutterWidth: Float) -> Unit = { state, viewport, isFocused, gutterWidth ->
        EditorCanvas(
            state = state,
            viewport = viewport,
            scrollController = scrollController,
            font = font,
            theme = theme,
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

    val contentHeight by remember(state.foldMap.totalVisualLines, font.lineHeight, dimensions) {
        derivedStateOf {
            state.foldMap.totalVisualLines * font.lineHeight + dimensions.contentPaddingBottom
        }
    }

    LaunchedEffect(state.primarySelection.caret, font, state.foldMap, dimensions, gutterWidth) {
        val visualLine = state.foldMap.documentToVisualLine(state.primarySelection.caret.line)

        scrollController.scrollToCaret(
            target = state.primarySelection.caret.copy(line = visualLine),
            charWidth = font.charWidth,
            lineHeight = font.lineHeight,
            gutterWidth = gutterWidth,
            editorPaddingStart = dimensions.editorPaddingStart,
        )
    }

    fun handleCopy() {
        val activeSelections = state.selections.filter(Selection::isNotEmpty)

        if (activeSelections.isNotEmpty()) {
            val text = activeSelections.joinToString("\n") { selection ->
                state.snapshot.getTextInRange(selection.range)
            }

            clipboardManager.setText(AnnotatedString(text))
        }
    }

    fun handleCut() {
        if (readOnly) return

        handleCopy()

        engine.dispatch(EditAction.Backspace)
    }

    fun handlePaste() {
        if (readOnly) return

        val textToPaste = clipboardManager.getText()?.text ?: return

        engine.dispatch(EditAction.Paste(textToPaste))
    }

    BoxWithConstraints(modifier = modifier) {
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
            engine = engine,
            scrollController = scrollController,
            fontCharWidth = font.charWidth,
            fontLineHeight = font.lineHeight,
            gutterWidth = gutterWidth,
            dimensions = dimensions,
            readOnly = readOnly,
            focusRequester = focusRequester,
            onFocusChanged = { isFocused = it },
            onContextMenu = { clickOffset, textPos ->
                menuData = EditorMenuData(
                    x = clickOffset.x,
                    y = clickOffset.y,
                    position = textPos,
                    selectedRange = state.selection.range,
                    selectedText = state.snapshot.getTextInRange(state.selection.range),
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
            handleCopy = ::handleCopy,
            handleCut = ::handleCut,
            handlePaste = ::handlePaste,
            keymap = keymap,
            onGutterClick = onGutterClick,
            modifier = Modifier.fillMaxSize(),
        ) {
            canvas(state, viewport, isFocused, gutterWidth)

            overlay()

            menuData?.let { data -> menu?.invoke(data) }
        }
    }
}