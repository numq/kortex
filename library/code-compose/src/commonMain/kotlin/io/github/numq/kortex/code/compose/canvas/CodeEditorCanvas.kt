package io.github.numq.kortex.code.compose.canvas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import io.github.numq.kortex.code.CodeEditorState
import io.github.numq.kortex.code.compose.layer.CodeLayerFactory
import io.github.numq.kortex.code.compose.theme.CodeEditorTheme
import io.github.numq.kortex.core.compose.canvas.SkiaCanvas
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.viewport.Viewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.skia.Rect
import kotlin.time.Duration.Companion.milliseconds

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

    var caretVisible by remember { mutableStateOf(true) }

    LaunchedEffect(state.selections, isFocused) {
        if (!isFocused) {
            caretVisible = false

            return@LaunchedEffect
        }

        caretVisible = true

        while (isActive) {
            delay(530L.milliseconds)

            caretVisible = !caretVisible
        }
    }

    val backgroundLayer = remember(viewport.width, viewport.height, theme) {
        layerFactory.createBackgroundLayer(viewport.width, viewport.height, theme.editor)
    }

    val currentLineLayer = remember(viewport.viewportLines, state.caret, theme) {
        layerFactory.createHighlightedLineLayer(viewport.viewportLines, state.caret, theme.editor)
    }

    val lineNumbersLayers = remember(viewport.viewportLines, gutterWidth, font, theme) {
        when {
            gutterWidth <= 0f -> emptyList()

            else -> viewport.viewportLines.map { viewportLine ->
                layerFactory.createGutterLineLayer(
                    viewportLine.line, gutterWidth, viewportLine.textBaselineY, font, theme.editor
                )
            }
        }
    }

    val gutterDividerLayer = remember(viewport.height, gutterWidth, theme) {
        when {
            gutterWidth <= 0f -> null

            else -> layerFactory.createGutterSeparatorLayer(gutterWidth, viewport.height, theme.editor)
        }
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

    val rulerLayer = remember(columnRuler, viewport.height, scrollController.horizontalOffset, font, theme) {
        columnRuler?.let { column ->
            layerFactory.createColumnRulerLayer(column, viewport.height, scrollController.horizontalOffset, font, theme)
        }
    }

    val contentLayers = remember(
        viewport.viewportLines, codeState.syntax?.tokensPerLine, scrollController.horizontalOffset, font, theme
    ) {
        layerFactory.createSyntaxContentLayers(
            viewportLines = viewport.viewportLines,
            tokensPerLine = codeState.syntax?.tokensPerLine,
            scrollX = scrollController.horizontalOffset,
            font = font,
            theme = theme,
        )
    }

    val occurrenceLayers = remember(
        state.caret, codeState.syntax?.occurrences, scrollController.horizontalOffset, contentLayers, theme
    ) {
        codeState.syntax?.occurrences?.let { occurrences ->
            layerFactory.createOccurrencesLayers(
                contentLayers, occurrences, state.caret, scrollController.horizontalOffset, theme
            )
        } ?: emptyList()
    }

    val diagnosticsLayers = remember(
        codeState.analysis?.issues, scrollController.horizontalOffset, contentLayers, theme
    ) {
        codeState.analysis?.issues?.let { issues ->
            layerFactory.createDiagnosticsLayers(contentLayers, issues, scrollController.horizontalOffset, theme)
        } ?: emptyList()
    }

    val selectionLayers = remember(state.selections, scrollController.horizontalOffset, contentLayers, theme) {
        state.selections.filter(Selection::isNotEmpty).map { selection ->
            layerFactory.createSelectionLayer(contentLayers, selection, scrollController.horizontalOffset, theme.editor)
        }
    }

    val caretLayers = remember(state.selections, scrollController.horizontalOffset, contentLayers, font, theme) {
        layerFactory.createCaretLayers(
            contentLayers, state.selections, scrollController.horizontalOffset, font, theme.editor
        )
    }

    SkiaCanvas(
        modifier = modifier.fillMaxSize(), onRender = { nativeCanvas, width, height ->
            nativeCanvas.save()

            nativeCanvas.clipRect(Rect.makeWH(width.toFloat(), height.toFloat()))

            nativeCanvas.clear(theme.editor.background.toArgb())

            backgroundLayer.render(nativeCanvas)

            currentLineLayer?.render(nativeCanvas)

            lineNumbersLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            foldingMarkersLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            gutterDividerLayer?.render(nativeCanvas)

            nativeCanvas.save()

            nativeCanvas.translate(gutterWidth, 0f)

            nativeCanvas.clipRect(Rect.makeWH(width.toFloat() - gutterWidth, height.toFloat()))

            rulerLayer?.render(nativeCanvas)

            if (state.selections.all(Selection::isEmpty)) {
                occurrenceLayers.forEach { layer ->
                    layer.render(nativeCanvas)
                }
            }

            selectionLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            contentLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            diagnosticsLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            if (caretVisible) {
                caretLayers.forEach { layer ->
                    layer.render(nativeCanvas)
                }
            }

            nativeCanvas.restore()

            nativeCanvas.restore()
        })
}