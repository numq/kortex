package io.github.numq.kortex.core.compose.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.Canvas

@Composable
actual fun SkiaCanvas(
    modifier: Modifier, onRender: (canvas: Canvas, width: Int, height: Int) -> Unit
) {
    Box(
        modifier = modifier.drawWithCache {
            val width = size.width.toInt()

            val height = size.height.toInt()

            onDrawBehind {
                if (width <= 0 || height <= 0) return@onDrawBehind

                drawIntoCanvas { canvas ->
                    onRender(canvas.nativeCanvas, width, height)
                }
            }
        })
}