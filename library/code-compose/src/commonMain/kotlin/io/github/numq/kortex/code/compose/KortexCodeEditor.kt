// file: library/code-compose/src/commonMain/kotlin/io/github/numq/kortex/code/compose/KortexCodeEditor.kt
package io.github.numq.kortex.code.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.numq.kortex.code.CodeEditorAction
import io.github.numq.kortex.code.CodeEditorEngine
import io.github.numq.kortex.code.CodeEditorState
import io.github.numq.kortex.code.compose.canvas.CodeEditorCanvas
import io.github.numq.kortex.code.compose.keymap.DefaultCodeEditorKeymap
import io.github.numq.kortex.code.compose.layer.CodeLayerFactory
import io.github.numq.kortex.code.compose.layer.rememberCodeLayerFactory
import io.github.numq.kortex.code.compose.overlay.CompletionOverlay
import io.github.numq.kortex.code.compose.theme.CodeEditorTheme
import io.github.numq.kortex.core.compose.KortexEditor
import io.github.numq.kortex.core.compose.dimensions.EditorDimensions
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.keymap.EditorKeymap
import io.github.numq.kortex.core.compose.menu.DefaultEditorMenu
import io.github.numq.kortex.core.compose.menu.EditorMenuData
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.compose.scroll.rememberEditorScrollController
import io.github.numq.kortex.core.viewport.ViewportAction

@Composable
fun KortexCodeEditor(
    engine: CodeEditorEngine,
    font: EditorFont,
    theme: CodeEditorTheme,
    modifier: Modifier = Modifier,
    showGutter: Boolean = true,
    readOnly: Boolean = false,
    columnRuler: Int? = 80,
    dimensions: EditorDimensions = remember { EditorDimensions() },
    scrollController: EditorScrollController = rememberEditorScrollController(),
    keymap: EditorKeymap = remember(engine) { DefaultCodeEditorKeymap.create(engine) },
    layerFactory: CodeLayerFactory = rememberCodeLayerFactory(dimensions = dimensions),
    overlay: @Composable () -> Unit = {},
    menu: (@Composable (EditorMenuData) -> Unit)? = { data ->
        DefaultEditorMenu(data = data)
    },
) {
    val codeState by engine.state.collectAsState()

    KortexEditor(
        engine = engine.engine,
        font = font,
        theme = theme.editor,
        modifier = modifier,
        showGutter = showGutter,
        readOnly = readOnly,
        dimensions = dimensions,
        scrollController = scrollController,
        keymap = keymap,
        layerFactory = layerFactory,
        menu = menu,
        onGutterClick = { line, _ ->
            when (codeState.syntax?.foldingRegions?.any { region ->
                region.range.start.line == line
            }) {
                true -> {
                    engine.engine.dispatch(ViewportAction.ToggleFolding(line))

                    true
                }

                else -> false
            }
        },
        canvas = { _, viewport, isFocused, gutterWidth ->
            CodeEditorCanvas(
                codeState = codeState,
                viewport = viewport,
                scrollController = scrollController,
                font = font,
                theme = theme,
                layerFactory = layerFactory,
                gutterWidth = gutterWidth,
                columnRuler = columnRuler,
                isFocused = isFocused,
                modifier = Modifier.fillMaxSize(),
            )
        },
        overlay = {
            overlay()

            (codeState.completion as? CodeEditorState.CompletionState.Visible)?.let { visibleCompletion ->
                CompletionOverlay(
                    completion = visibleCompletion,
                    foldMap = codeState.foldMap,
                    font = font,
                    scrollController = scrollController,
                    onSelect = { engine.dispatch(CodeEditorAction.Completion.Apply) },
                    onDismiss = { engine.dispatch(CodeEditorAction.Completion.Dismiss) },
                )
            }
        })
}