package io.github.numq.kortex.code.compose.gutter

import io.github.numq.kortex.core.compose.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

/**
 * Skia layer rendering a folding marker (arrow/chevron) in the gutter.
 *
 * @property line 0-based document line index where the fold starts.
 * @property rect Bounding box of the marker in gutter pixel coordinates.
 * @property isCollapsed True if the region is currently collapsed.
 */
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

        Path().use { path ->
            when {
                isCollapsed -> {
                    path.moveTo(cx - size * .5f, cy - size)
                    path.lineTo(cx + size * .6f, cy)
                    path.lineTo(cx - size * .5f, cy + size)
                }

                else -> {
                    path.moveTo(cx - size, cy - size * .5f)
                    path.lineTo(cx, cy + size * .6f)
                    path.lineTo(cx + size, cy - size * .5f)
                }
            }

            path.closePath()
            canvas.drawPath(path, paint)
        }
    }
}