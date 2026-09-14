package io.github.numq.kortex.core.compose.cache

import io.github.numq.kortex.core.cache.LruCache
import io.github.numq.kortex.core.compose.font.EditorFont
import org.jetbrains.skia.TextLine

class TextLineCache(capacity: Int) : LruCache<TextLineCache.Key, TextLine>(capacity) {
    data class Key(val text: String, val font: EditorFont)

    override val factory: Key.() -> TextLine = {
        font.createTextLine(text = text)
    }
}