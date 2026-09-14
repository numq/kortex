package io.github.numq.kortex.core.compose.gutter

import io.github.numq.kortex.core.compose.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.TextLine

data class GutterLineLayer(
    val line: Int,
    val text: String,
    val textLine: TextLine,
    val paint: Paint,
    val textX: Float,
    val textY: Float,
    val isCurrentLine: Boolean = false,
) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            canvas.drawTextLine(line = textLine, x = textX, y = textY, paint = paint)
        }
    }
}