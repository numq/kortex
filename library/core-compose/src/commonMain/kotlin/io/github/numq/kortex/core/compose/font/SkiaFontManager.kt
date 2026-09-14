package io.github.numq.kortex.core.compose.font

import io.github.numq.kortex.core.lifecycle.CloseableResource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface
import org.jetbrains.skia.impl.use

internal class SkiaFontManager : CloseableResource(), FontManager {
    private val lock = SynchronizedObject()

    private data class FontKey(val size: Int, val hash: Int)

    private val typefaceCache = HashMap<FontKey, Typeface>()

    override fun loadFontFromBytes(
        bytes: ByteArray,
        size: Float,
        lineSpacing: Float,
    ): Result<EditorFont> = runCatching {
        checkOpen()

        val key = FontKey(bytes.size, bytes.contentHashCode())

        val typeface = synchronized(lock) {
            typefaceCache.getOrPut(key) {
                Data.makeFromBytes(bytes).use { data ->
                    checkNotNull(FontMgr.default.makeFromData(data)) {
                        "Failed to parse Typeface from font data"
                    }
                }
            }
        }

        EditorFont(typeface = typeface, size = size, lineSpacing = lineSpacing)
    }

    override fun onRelease() {
        synchronized(lock) {
            typefaceCache.values.forEach { typeface ->
                if (!typeface.isClosed) {
                    typeface.close()
                }
            }

            typefaceCache.clear()
        }
    }
}