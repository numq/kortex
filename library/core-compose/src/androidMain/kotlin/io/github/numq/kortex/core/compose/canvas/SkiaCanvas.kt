package io.github.numq.kortex.core.compose.canvas

import android.graphics.PixelFormat
import android.view.SurfaceHolder
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skiko.SkikoSurfaceView
import java.util.concurrent.atomic.AtomicReference

@Composable
actual fun SkiaCanvas(modifier: Modifier, onRender: (canvas: Canvas, width: Int, height: Int) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val onRenderRef = remember { AtomicReference(onRender) }

    onRenderRef.set(onRender)

    var surfaceView by remember { mutableStateOf<SkikoSurfaceView?>(null) }

    val layer = remember {
        SkiaLayer().apply {
            renderDelegate = object : SkikoRenderDelegate {
                override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                    if (width <= 0 || height <= 0) return

                    val scale = contentScale.takeIf { it > 0f } ?: 1f

                    val scaledWidth = (width / scale).toInt()

                    val scaledHeight = (height / scale).toInt()

                    if (scaledWidth > 0 && scaledHeight > 0) {
                        val saveCount = canvas.save()

                        try {
                            canvas.scale(scale, scale)

                            onRenderRef.get().invoke(canvas, scaledWidth, scaledHeight)
                        } finally {
                            canvas.restoreToCount(saveCount)
                        }
                    }
                }
            }
        }
    }

    SideEffect {
        surfaceView?.scheduleFrame()
    }

    DisposableEffect(lifecycleOwner, surfaceView, layer) {
        val view = surfaceView ?: return@DisposableEffect onDispose {}

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    view.onResume()

                    view.scheduleFrame()
                }

                Lifecycle.Event.ON_PAUSE -> view.onPause()

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)

            view.onPause()
        }
    }

    DisposableEffect(layer) {
        onDispose {
            surfaceView = null

            layer.detach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            SkikoSurfaceView(context, layer).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )

                setZOrderMediaOverlay(true)

                holder.setFormat(PixelFormat.TRANSLUCENT)

                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        scheduleFrame()
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        scheduleFrame()
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
                })

                surfaceView = this
            }
        },
        update = { view ->
            view.scheduleFrame()
        },
    )
}