package io.github.numq.kortex.core.compose.canvas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.layer.Layer
import io.github.numq.kortex.core.compose.layer.LayerFactory
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.compose.text.TextContentLayer
import io.github.numq.kortex.core.compose.theme.EditorTheme
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.viewport.Viewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.skia.Rect
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun EditorCanvas(
    state: EditorState,
    font: EditorFont,
    theme: EditorTheme,
    scrollController: EditorScrollController,
    viewport: Viewport,
    layerFactory: LayerFactory,
    gutterWidth: Float,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    contentLayers: List<TextContentLayer> = remember(
        viewport.viewportLines, scrollController.horizontalOffset, font, theme
    ) {
        layerFactory.createCodeAreaContentLayers(
            viewport.viewportLines, scrollController.horizontalOffset, font, theme
        )
    },
    extraGutterLayers: List<Layer> = emptyList(),
    underlayLayers: List<Layer> = emptyList(),
    overlayLayers: List<Layer> = emptyList(),
) {
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

    val showCarets = isFocused && caretVisible

    val backgroundLayer = remember(viewport.width, viewport.height, theme) {
        layerFactory.createBackgroundLayer(viewport.width, viewport.height, theme)
    }

    val gutterBackgroundLayer = remember(gutterWidth, viewport.height, theme) {
        layerFactory.createGutterBackgroundLayer(gutterWidth, viewport.height, theme)
    }

    val highlightedLineLayer = remember(viewport.viewportLines, state.caret, theme) {
        layerFactory.createHighlightedLineLayer(viewport.viewportLines, state.caret, theme)
    }

    val gutterLineLayers = remember(viewport.viewportLines, gutterWidth, state.caret.position.line, font, theme) {
        viewport.viewportLines.map { viewportLine ->
            layerFactory.createGutterLineLayer(
                line = viewportLine.line,
                width = gutterWidth,
                textY = viewportLine.textBaselineY,
                font = font,
                theme = theme,
                isCurrentLine = viewportLine.line == state.caret.position.line,
            )
        }
    }

    val gutterSeparatorLayer = remember(viewport.height, gutterWidth, theme) {
        layerFactory.createGutterSeparatorLayer(gutterWidth, viewport.height, theme)
    }

    val selectionLayers = remember(state.selections, scrollController.horizontalOffset, contentLayers, theme) {
        state.selections.filter(Selection::isNotEmpty).map { selection ->
            layerFactory.createSelectionLayer(
                contentLayers = contentLayers,
                selection = selection,
                scrollX = scrollController.horizontalOffset,
                theme = theme,
            )
        }
    }

    val caretLayers = remember(state.selections, scrollController.horizontalOffset, contentLayers, font, theme) {
        layerFactory.createCaretLayers(
            contentLayers = contentLayers,
            selections = state.selections,
            scrollX = scrollController.horizontalOffset,
            font = font,
            theme = theme,
        )
    }

    SkiaCanvas(
        modifier = modifier.fillMaxSize(),
        onRender = { nativeCanvas, width, height ->
            val canvasW = width.toFloat()
            val canvasH = height.toFloat()

            nativeCanvas.save()
            nativeCanvas.clipRect(Rect.makeWH(canvasW, canvasH))
            nativeCanvas.clear(theme.background.toArgb())

            backgroundLayer.render(nativeCanvas)

            if (gutterWidth > 0f) {
                gutterBackgroundLayer?.render(nativeCanvas)
            }

            highlightedLineLayer?.render(nativeCanvas)

            if (gutterWidth > 0f) {
                nativeCanvas.save()
                nativeCanvas.clipRect(Rect.makeWH(gutterWidth, canvasH))

                gutterLineLayers.forEach { layer ->
                    layer.render(nativeCanvas)
                }

                extraGutterLayers.forEach { layer ->
                    layer.render(nativeCanvas)
                }

                nativeCanvas.restore()
                gutterSeparatorLayer.render(nativeCanvas)
            }

            nativeCanvas.save()
            nativeCanvas.translate(gutterWidth, 0f)
            nativeCanvas.clipRect(Rect.makeWH((canvasW - gutterWidth).coerceAtLeast(0f), canvasH))

            underlayLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            selectionLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            contentLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            overlayLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            if (showCarets) {
                caretLayers.forEach { layer ->
                    layer.render(nativeCanvas)
                }
            }

            nativeCanvas.restore()
            nativeCanvas.restore()
        },
    )
}