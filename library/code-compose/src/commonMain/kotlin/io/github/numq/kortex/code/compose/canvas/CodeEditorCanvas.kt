package io.github.numq.kortex.code.compose.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.numq.kortex.code.CodeEditorState
import io.github.numq.kortex.code.compose.layer.CodeLayerFactory
import io.github.numq.kortex.code.compose.theme.CodeEditorTheme
import io.github.numq.kortex.core.compose.canvas.EditorCanvas
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.viewport.Viewport

@Composable
internal fun CodeEditorCanvas(
    codeState: CodeEditorState,
    viewport: Viewport,
    scrollController: EditorScrollController,
    font: EditorFont,
    theme: CodeEditorTheme,
    layerFactory: CodeLayerFactory,
    gutterWidth: Float,
    columnRuler: Int?,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = codeState.editor

    val scrollX = scrollController.horizontalOffset

    val contentLayers = remember(
        viewport.viewportLines, codeState.syntax?.tokensPerLine, scrollX, font, theme
    ) {
        layerFactory.createSyntaxContentLayers(
            viewportLines = viewport.viewportLines,
            tokensPerLine = codeState.syntax?.tokensPerLine,
            scrollX = scrollX,
            font = font,
            theme = theme,
        )
    }

    val foldingMarkersLayers = remember(
        viewport.viewportLines, gutterWidth, codeState.syntax?.foldingRegions, state.collapsedLines, theme
    ) {
        when {
            gutterWidth <= 0f -> emptyList()

            else -> layerFactory.createFoldingMarkersLayers(
                viewportLines = viewport.viewportLines,
                gutterWidth = gutterWidth,
                foldingRegions = codeState.syntax?.foldingRegions,
                collapsedLines = state.collapsedLines,
                theme = theme
            )
        }
    }

    val rulerLayer = remember(columnRuler, viewport.height, scrollX, font, theme) {
        columnRuler?.let { col ->
            layerFactory.createColumnRulerLayer(col, viewport.height, scrollX, font, theme)
        }
    }

    val occurrenceLayers = remember(
        state.caret, codeState.syntax?.occurrences, scrollX, contentLayers, theme
    ) {
        codeState.syntax?.occurrences?.let { occurrences ->
            layerFactory.createOccurrencesLayers(contentLayers, occurrences, state.caret, scrollX, theme)
        } ?: emptyList()
    }

    val diagnosticsLayers = remember(
        codeState.analysis?.issues, scrollX, contentLayers, theme
    ) {
        codeState.analysis?.issues?.let { issues ->
            layerFactory.createDiagnosticsLayers(contentLayers, issues, scrollX, theme)
        } ?: emptyList()
    }

    val underlayLayers = remember(rulerLayer, occurrenceLayers, state.selections) {
        buildList {
            rulerLayer?.let(::add)

            if (state.selections.all(Selection::isEmpty)) {
                addAll(occurrenceLayers)
            }
        }
    }

    EditorCanvas(
        state = state,
        font = font,
        theme = theme.editor,
        scrollController = scrollController,
        viewport = viewport,
        layerFactory = layerFactory,
        gutterWidth = gutterWidth,
        isFocused = isFocused,
        modifier = modifier,
        contentLayers = contentLayers,
        extraGutterLayers = foldingMarkersLayers,
        underlayLayers = underlayLayers,
        overlayLayers = diagnosticsLayers,
    )
}