package io.github.numq.kortex.core.compose.caret

import io.github.numq.kortex.core.compose.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

data class CaretLayer(val x: Float, val y: Float, val height: Float, val width: Float, val paint: Paint) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            val bounds = Rect.makeXYWH(l = x, t = y, w = width, h = height)

            canvas.drawRect(r = bounds, paint = paint)
        }
    }
}