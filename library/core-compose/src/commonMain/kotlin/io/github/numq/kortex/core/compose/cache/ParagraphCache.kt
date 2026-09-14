package io.github.numq.kortex.core.compose.cache

import io.github.numq.kortex.core.cache.LruCache
import io.github.numq.kortex.core.compose.font.EditorFont
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle

class ParagraphCache(capacity: Int = 1000) : LruCache<ParagraphCache.Key, Paragraph>(capacity) {
    data class Key(val text: String, val font: EditorFont, val textColor: Int)

    override val factory: Key.() -> Paragraph = {
        val baseTextStyle = TextStyle().apply {
            setFontFamily(font.familyName)

            this.fontSize = font.size

            this.color = textColor
        }

        val style = ParagraphStyle().apply {
            this.maxLinesCount = 1

            this.textStyle = baseTextStyle
        }

        val builder = font.buildParagraph(style) {
            pushStyle(baseTextStyle)

            addText(text)

            popStyle()
        }

        builder.build().apply { layout(Float.MAX_VALUE) }
    }
}