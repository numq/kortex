package io.github.numq.kortex.code.compose.cache

import androidx.compose.ui.graphics.toArgb
import io.github.numq.kortex.code.compose.theme.SyntaxTheme
import io.github.numq.kortex.code.token.Token
import io.github.numq.kortex.core.cache.LruCache
import io.github.numq.kortex.core.compose.font.EditorFont
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TextStyle

internal class CodeParagraphCache(capacity: Int = 1000) : LruCache<CodeParagraphCache.Key, Paragraph>(capacity) {
    data class Key(
        val line: Int,
        val text: String,
        val tokens: List<Token>,
        val font: EditorFont,
        val syntaxTheme: SyntaxTheme,
    )

    override val factory: Key.() -> Paragraph = {
        val defaultColor = syntaxTheme.defaultText.toArgb()

        val baseTextStyle = TextStyle().apply {
            setFontFamily(font.familyName)

            this.fontSize = font.size
        }

        val style = ParagraphStyle().apply {
            this.maxLinesCount = 1

            this.textStyle = baseTextStyle
        }

        val builder = font.buildParagraph(style) {
            fun createStyle(color: Int) = TextStyle().apply {
                this.fontFamilies = baseTextStyle.fontFamilies

                this.fontSize = baseTextStyle.fontSize

                this.color = color
            }

            when {
                tokens.isEmpty() -> {
                    pushStyle(createStyle(defaultColor))

                    addText(text)

                    popStyle()
                }

                else -> {
                    var currentIndex = 0

                    val sortedTokens = tokens.filter { token ->
                        token.range.start.line <= line && token.range.end.line >= line
                    }.sortedBy { token ->
                        when (token.range.start.line) {
                            line -> token.range.start.column

                            else -> 0
                        }
                    }

                    for ((range, type) in sortedTokens) {
                        val start = when {
                            range.start.line < line -> 0

                            else -> range.start.column.coerceIn(0, text.length)
                        }

                        val end = when {
                            range.end.line > line -> text.length

                            else -> range.end.column.coerceIn(start, text.length)
                        }

                        if (start > currentIndex) {
                            pushStyle(createStyle(defaultColor))

                            addText(text.substring(currentIndex, start))

                            popStyle()
                        }

                        if (end > start) {
                            val tokenColor = syntaxTheme.resolver.resolveColor(type, syntaxTheme.defaultText).toArgb()

                            pushStyle(createStyle(tokenColor))

                            addText(text.substring(start, end))

                            popStyle()

                            currentIndex = end
                        }
                    }

                    if (currentIndex < text.length) {
                        pushStyle(createStyle(defaultColor))

                        addText(text.substring(currentIndex))

                        popStyle()
                    }
                }
            }
        }

        builder.build().apply { layout(Float.MAX_VALUE) }
    }
}