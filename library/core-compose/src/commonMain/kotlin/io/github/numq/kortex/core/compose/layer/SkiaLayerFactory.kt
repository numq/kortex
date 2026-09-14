package io.github.numq.kortex.core.compose.layer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import io.github.numq.kortex.core.caret.Caret
import io.github.numq.kortex.core.compose.background.BackgroundLayer
import io.github.numq.kortex.core.compose.background.HighlightedLineLayer
import io.github.numq.kortex.core.compose.cache.PaintCache
import io.github.numq.kortex.core.compose.cache.ParagraphCache
import io.github.numq.kortex.core.compose.cache.TextLineCache
import io.github.numq.kortex.core.compose.caret.CaretLayer
import io.github.numq.kortex.core.compose.dimensions.EditorDimensions
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.gutter.GutterLineLayer
import io.github.numq.kortex.core.compose.gutter.GutterSeparatorLayer
import io.github.numq.kortex.core.compose.selection.SelectionLayer
import io.github.numq.kortex.core.compose.selection.SelectionRegionLayer
import io.github.numq.kortex.core.compose.text.TextContentLayer
import io.github.numq.kortex.core.compose.theme.EditorTheme
import io.github.numq.kortex.core.lifecycle.CloseableResource
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.viewport.ViewportLine

class SkiaLayerFactory(
    private val textLineCache: TextLineCache,
    private val paintCache: PaintCache,
    private val paragraphCache: ParagraphCache,
    private val dimensions: EditorDimensions,
) : CloseableResource(), LayerFactory {
    override fun createBackgroundLayer(width: Float, height: Float, theme: EditorTheme) = BackgroundLayer(
        width = width, height = height, paint = paintCache.getOrCreate(
            PaintCache.Key(color = theme.background.toArgb())
        ).getOrThrow()
    )

    override fun createHighlightedLineLayer(
        viewportLines: List<ViewportLine>,
        caret: Caret,
        theme: EditorTheme,
    ): HighlightedLineLayer? {
        val viewportLine = viewportLines.firstOrNull { viewportLine ->
            viewportLine.line == caret.position.line
        } ?: return null

        return HighlightedLineLayer(
            x = viewportLine.x,
            y = viewportLine.y,
            width = viewportLine.width,
            height = viewportLine.height,
            paint = paintCache.getOrCreate(
                PaintCache.Key(color = theme.currentLine.toArgb())
            ).getOrThrow()
        )
    }

    override fun createGutterLineLayer(
        line: Int,
        width: Float,
        textY: Float,
        font: EditorFont,
        theme: EditorTheme,
    ): GutterLineLayer {
        val text = "${line + 1}"

        val textLine = textLineCache.getOrCreate(
            TextLineCache.Key(text = text, font = font)
        ).getOrThrow()

        val textPaint = paintCache.getOrCreate(
            PaintCache.Key(color = theme.gutter.text.toArgb())
        ).getOrThrow()

        val textX = width - font.lineHeight - dimensions.gutterGap - dimensions.gutterPaddingEnd - textLine.width

        return GutterLineLayer(
            line = line, text = text, textLine = textLine, paint = textPaint, textX = textX, textY = textY
        )
    }

    override fun createGutterSeparatorLayer(x: Float, height: Float, theme: EditorTheme) = GutterSeparatorLayer(
        x = x, height = height, paint = paintCache.getOrCreate(
            PaintCache.Key(color = theme.gutter.divider.toArgb())
        ).getOrThrow()
    )

    override fun createCodeAreaContentLayers(
        viewportLines: List<ViewportLine>,
        scrollX: Float,
        font: EditorFont,
        theme: EditorTheme,
    ) = viewportLines.map { viewportLine ->
        val paragraph = paragraphCache.getOrCreate(
            ParagraphCache.Key(
                text = viewportLine.text, font = font, textColor = theme.text.toArgb()
            )
        ).getOrThrow()

        TextContentLayer(
            viewportLine = viewportLine,
            paragraph = paragraph,
            x = -scrollX + dimensions.editorPaddingStart,
            y = viewportLine.textBaselineY + font.ascent
        )
    }

    override fun createSelectionLayer(
        contentLayers: List<TextContentLayer>,
        selection: Selection,
        scrollX: Float,
        theme: EditorTheme,
    ): SelectionLayer {
        if (selection.range.isEmpty) return SelectionLayer()

        val selectionPaint = paintCache.getOrCreate(
            PaintCache.Key(color = theme.selection.toArgb())
        ).getOrThrow()

        val (start, end) = selection.range

        val selectionRegionLayers = mutableListOf<SelectionRegionLayer>()

        contentLayers.filter { contentLayer ->
            contentLayer.viewportLine.line in start.line..end.line
        }.forEach { layer ->
            val line = layer.viewportLine.line

            val lineLength = layer.viewportLine.text.length

            val relStartX = if (line == start.line) {
                layer.getCoordinateAtOffset(start.column.coerceIn(0, lineLength))
            } else 0f

            val relEndX = when (line) {
                end.line -> layer.getCoordinateAtOffset(end.column.coerceIn(0, lineLength))

                else -> layer.viewportLine.width + scrollX
            }

            val baseOffset = -scrollX + dimensions.editorPaddingStart

            val left = (baseOffset + relStartX).coerceIn(0f, layer.viewportLine.width)

            val right = (baseOffset + relEndX).coerceIn(0f, layer.viewportLine.width)

            if (left < right) {
                selectionRegionLayers.add(
                    SelectionRegionLayer(
                        left = left,
                        top = layer.viewportLine.y,
                        right = right,
                        bottom = layer.viewportLine.y + layer.viewportLine.height,
                        paint = selectionPaint
                    )
                )
            }
        }

        return SelectionLayer(selectionRegionLayers = selectionRegionLayers)
    }

    override fun createCaretLayers(
        contentLayers: List<TextContentLayer>,
        selections: List<Selection>,
        scrollX: Float,
        font: EditorFont,
        theme: EditorTheme,
    ): List<CaretLayer> {
        val paint = paintCache.getOrCreate(
            PaintCache.Key(color = theme.caret.toArgb())
        ).getOrThrow()

        return selections.mapNotNull { selection ->
            val contentLayer = contentLayers.firstOrNull { contentLayer ->
                contentLayer.viewportLine.line == selection.caret.line
            } ?: return@mapNotNull null

            val xOffset = contentLayer.getCoordinateAtOffset(offset = selection.caret.column).takeIf { coordinate ->
                coordinate > 0f || selection.caret.column == 0
            } ?: (selection.caret.column * font.charWidth)

            CaretLayer(
                x = -scrollX + xOffset + dimensions.editorPaddingStart,
                y = contentLayer.viewportLine.textBaselineY + font.ascent,
                height = font.textHeight,
                width = dimensions.caretWidth,
                paint = paint
            )
        }
    }

    override fun onRelease() {
        textLineCache.close()

        paintCache.close()

        paragraphCache.close()
    }
}

@Composable
fun rememberLayerFactory(dimensions: EditorDimensions): LayerFactory {
    val factory = remember(dimensions) {
        SkiaLayerFactory(
            textLineCache = TextLineCache(1000),
            paintCache = PaintCache(1000),
            paragraphCache = ParagraphCache(1000),
            dimensions = dimensions,
        )
    }

    DisposableEffect(factory) {
        onDispose {
            factory.close()
        }
    }

    return factory
}