package io.github.numq.kortex.code.compose.occurrences

import io.github.numq.kortex.core.compose.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

data class OccurrencesHighlightLayer(
    val rect: Rect,
    val paint: Paint,
) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed) {
            canvas.drawRect(r = rect, paint = paint)
        }
    }
}