package io.github.numq.kortex.core.compose.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skiko.SkikoSurfaceView

@Composable
actual fun SkiaCanvas(
    modifier: Modifier, onRender: (canvas: Canvas, width: Int, height: Int) -> Unit
) {
    val renderDelegate = remember(onRender) {
        object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                onRender(canvas, width, height)
            }
        }
    }

    val layer = remember {
        SkiaLayer().apply {
            this.renderDelegate = renderDelegate
        }
    }

    AndroidView(modifier = modifier, factory = { context ->
        SkikoSurfaceView(context, layer).apply {
            layer.renderDelegate = renderDelegate
        }
    }, update = {
        layer.renderDelegate = renderDelegate

        layer.needRedraw()
    })
}