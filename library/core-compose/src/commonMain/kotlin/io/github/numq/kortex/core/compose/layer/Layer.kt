package io.github.numq.kortex.core.compose.layer

import org.jetbrains.skia.Canvas

interface Layer {
    fun render(canvas: Canvas)
}