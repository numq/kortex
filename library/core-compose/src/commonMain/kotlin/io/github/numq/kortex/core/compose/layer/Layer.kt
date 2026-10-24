package io.github.numq.kortex.core.compose.layer

import org.jetbrains.skia.Canvas

/**
 * Visual element rendered onto a Skia [Canvas].
 */
interface Layer {
    fun render(canvas: Canvas)
}