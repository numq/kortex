package io.github.numq.kortex.core.compose.canvas

import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.ViewGroup
import android.widget.FrameLayout
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
actual fun SkiaCanvas(
    modifier: Modifier,
    onRender: (canvas: Canvas, width: Int, height: Int) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val onRenderRef = remember { AtomicReference(onRender) }
    onRenderRef.set(onRender)

    var surfaceView by remember { mutableStateOf<SkikoSurfaceView?>(null) }

    val layer = remember {
        SkiaLayer().apply {
            renderDelegate = object : SkikoRenderDelegate {
                override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                    if (width <= 0 || height <= 0) return

                    val saveCount = canvas.save()
                    try {
                        onRenderRef.get().invoke(canvas, width, height)
                    } finally {
                        canvas.restoreToCount(saveCount)
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
        factory = { ctx ->
            val skikoView = SkikoSurfaceView(ctx, layer).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )

                isFocusable = false
                isFocusableInTouchMode = false
                isClickable = false

                holder.addCallback(object : SurfaceHolder.Callback2 {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        scheduleFrame()
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        scheduleFrame()
                    }

                    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                        scheduleFrame()
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
                })

                surfaceView = this
            }

            object : FrameLayout(ctx) {
                init {
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isClickable = false
                    addView(skikoView)
                }

                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    skikoView.scheduleFrame()
                }

                override fun dispatchTouchEvent(ev: MotionEvent?): Boolean = false
            }
        },
        update = {
            surfaceView?.scheduleFrame()
        },
    )
}