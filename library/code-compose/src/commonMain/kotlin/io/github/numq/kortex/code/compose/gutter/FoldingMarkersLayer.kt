package io.github.numq.kortex.code.compose.gutter

import io.github.numq.kortex.core.compose.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Rect

class FoldingMarkersLayer(
    val line: Int,
    val rect: Rect,
    val isCollapsed: Boolean,
    private val paint: Paint,
) : Layer {
    override fun render(canvas: Canvas) {
        if (paint.isClosed) return

        val cx = rect.left + rect.width / 2f

        val cy = rect.top + rect.height / 2f

        val size = rect.width * 0.35f

        val path = Path()

        when {
            isCollapsed -> {
                path.moveTo(cx - size * 0.5f, cy - size)

                path.lineTo(cx + size * 0.6f, cy)

                path.lineTo(cx - size * 0.5f, cy + size)
            }

            else -> {
                path.moveTo(cx - size, cy - size * 0.5f)

                path.lineTo(cx, cy + size * 0.6f)

                path.lineTo(cx + size, cy - size * 0.5f)
            }
        }

        path.closePath()

        canvas.drawPath(path, paint)

        path.close()
    }
}