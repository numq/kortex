package io.github.numq.kortex.core.compose.canvas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.layer.LayerFactory
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
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
    viewport: Viewport,
    scrollController: EditorScrollController,
    font: EditorFont,
    theme: EditorTheme,
    layerFactory: LayerFactory,
    gutterWidth: Float,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
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

    val backgroundLayer = remember(viewport.width, viewport.height, theme) {
        layerFactory.createBackgroundLayer(viewport.width, viewport.height, theme)
    }

    val highlightedLineLayer = remember(viewport.viewportLines, state.caret, theme) {
        layerFactory.createHighlightedLineLayer(viewport.viewportLines, state.caret, theme)
    }

    val gutterLineLayers = remember(viewport.viewportLines, gutterWidth, font, theme) {
        viewport.viewportLines.map { viewportLine ->
            layerFactory.createGutterLineLayer(viewportLine.line, gutterWidth, viewportLine.textBaselineY, font, theme)
        }
    }

    val gutterSeparatorLayer = remember(viewport.height, gutterWidth, theme) {
        layerFactory.createGutterSeparatorLayer(gutterWidth, viewport.height, theme)
    }

    val contentLayers = remember(
        viewport.viewportLines, scrollController.horizontalOffset, font, theme
    ) {
        layerFactory.createCodeAreaContentLayers(
            viewport.viewportLines, scrollController.horizontalOffset, font, theme
        )
    }

    val selectionLayers = remember(state.selections, scrollController.horizontalOffset, contentLayers, theme) {
        state.selections.filter(Selection::isNotEmpty).map { selection ->
            layerFactory.createSelectionLayer(
                contentLayers = contentLayers,
                selection = selection,
                scrollX = scrollController.horizontalOffset,
                theme = theme
            )
        }
    }

    val caretLayers = remember(state.selections, scrollController.horizontalOffset, contentLayers, font, theme) {
        layerFactory.createCaretLayers(
            contentLayers = contentLayers,
            selections = state.selections,
            scrollX = scrollController.horizontalOffset,
            font = font,
            theme = theme
        )
    }

    SkiaCanvas(
        modifier = modifier.fillMaxSize(), onRender = { nativeCanvas, width, height ->
            nativeCanvas.save()

            nativeCanvas.clipRect(Rect.makeWH(width.toFloat(), height.toFloat()))

            nativeCanvas.clear(theme.background.toArgb())

            backgroundLayer.render(nativeCanvas)

            highlightedLineLayer?.render(nativeCanvas)

            gutterLineLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            gutterSeparatorLayer.render(nativeCanvas)

            nativeCanvas.save()
            nativeCanvas.translate(gutterWidth, 0f)
            nativeCanvas.clipRect(Rect.makeWH(width.toFloat() - gutterWidth, height.toFloat()))

            selectionLayers.forEach { layer ->
                layer.render(nativeCanvas)
            }

            contentLayers.forEach { layer ->
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