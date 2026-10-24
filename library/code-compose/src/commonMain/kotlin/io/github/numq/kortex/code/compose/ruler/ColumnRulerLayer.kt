package io.github.numq.kortex.code.compose.ruler

import io.github.numq.kortex.core.compose.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint

/**
 * Skia rendering layer that draws a vertical line at a specific character column.
 *
 * @property x Horizontal pixel coordinate of the ruler.
 * @property height Total visible height of the ruler.
 * @property paint Skia paint configuration for drawing the vertical guide.
 */
data class ColumnRulerLayer(
    val x: Float,
    val height: Float,
    val paint: Paint,
) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            canvas.drawLine(x0 = x, y0 = 0f, x1 = x, y1 = height, paint = paint)
        }
    }
}