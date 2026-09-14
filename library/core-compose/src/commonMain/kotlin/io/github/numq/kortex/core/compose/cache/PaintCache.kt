package io.github.numq.kortex.core.compose.cache

import io.github.numq.kortex.core.cache.LruCache
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode

class PaintCache(capacity: Int = 100) : LruCache<PaintCache.Key, Paint>(capacity) {
    data class Key(
        val color: Int,
        val isAntiAlias: Boolean = true,
        val mode: PaintMode = PaintMode.FILL,
        val strokeWidth: Float = 0f,
    )

    override val factory: Key.() -> Paint = {
        val (color, isAntiAlias, mode, strokeWidth) = this

        Paint().apply {
            this.color = color
            this.isAntiAlias = isAntiAlias
            this.mode = mode
            this.strokeWidth = strokeWidth
        }
    }
}