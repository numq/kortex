package io.github.numq.kortex.code.compose.diagnostics

import io.github.numq.kortex.core.compose.layer.Layer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import kotlin.math.max

class DiagnosticsLayer(
    private val startX: Float,
    private val endX: Float,
    private val baselineY: Float,
    private val paint: Paint,
) : Layer {
    override fun render(canvas: Canvas) {
        if (!paint.isClosed && startX < endX) {
            val y = baselineY + 3f

            val step = 4f

            val amplitude = 1.5f

            val distance = endX - startX

            val stepsCount = max(1, (distance / step).toInt())

            val actualStep = distance / stepsCount

            val path = Path()

            path.moveTo(startX, y)

            for (i in 1..stepsCount) {
                val currentX = startX + (i * actualStep)

                val currentY = when {
                    i % 2 != 0 -> y + amplitude

                    else -> y - amplitude
                }

                path.lineTo(currentX, currentY)
            }

            canvas.drawPath(path, paint)

            path.close()
        }
    }
}