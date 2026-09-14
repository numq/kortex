package io.github.numq.kortex.core.compose.text

import io.github.numq.kortex.core.compose.layer.Layer
import io.github.numq.kortex.core.viewport.ViewportLine
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.RectHeightMode
import org.jetbrains.skia.paragraph.RectWidthMode

data class TextContentLayer(
    val viewportLine: ViewportLine, val paragraph: Paragraph, val x: Float, val y: Float
) : Layer {
    fun getCoordinateAtOffset(offset: Int): Float {
        if (paragraph.isClosed || offset <= 0) return 0f

        val safeOffset = offset.coerceAtMost(viewportLine.text.length)

        if (safeOffset <= 0) return 0f

        val rects = paragraph.getRectsForRange(
            start = safeOffset - 1,
            end = safeOffset,
            rectHeightMode = RectHeightMode.TIGHT,
            rectWidthMode = RectWidthMode.TIGHT
        )

        return rects.firstOrNull()?.rect?.right ?: 0f
    }

    fun getOffsetAtCoordinate(targetX: Float): Int {
        if (paragraph.isClosed || targetX <= 0) return 0

        val localX = targetX - x

        return paragraph.getGlyphPositionAtCoordinate(localX, 0f).position
    }

    override fun render(canvas: Canvas) {
        if (!paragraph.isClosed) {
            paragraph.paint(canvas, x, y)
        }
    }
}