package io.github.numq.kortex.core.compose.text

import io.github.numq.kortex.core.compose.layer.Layer
import io.github.numq.kortex.core.viewport.ViewportLine
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.RectHeightMode
import org.jetbrains.skia.paragraph.RectWidthMode

data class TextContentLayer(
    val viewportLine: ViewportLine,
    val paragraph: Paragraph,
    val x: Float,
    val y: Float,
) : Layer {
    fun getCoordinateAtOffset(offset: Int): Float {
        if (paragraph.isClosed || offset <= 0) return 0f

        val text = viewportLine.text
        val safeOffset = offset.coerceAtMost(text.length)

        if (safeOffset <= 0) return 0f

        val startOffset = when {
            safeOffset >= 2 && text[safeOffset - 1].isLowSurrogate() && text[safeOffset - 2].isHighSurrogate() -> safeOffset - 2
            else -> safeOffset - 1
        }

        val rects = paragraph.getRectsForRange(
            start = startOffset,
            end = safeOffset,
            rectHeightMode = RectHeightMode.TIGHT,
            rectWidthMode = RectWidthMode.TIGHT,
        )

        return rects.firstOrNull()?.rect?.right ?: 0f
    }

    override fun render(canvas: Canvas) {
        if (!paragraph.isClosed) {
            paragraph.paint(canvas, x, y)
        }
    }
}