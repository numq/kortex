package io.github.numq.kortex.code.compose.cache

import androidx.compose.ui.graphics.toArgb
import io.github.numq.kortex.code.compose.theme.SyntaxTheme
import io.github.numq.kortex.code.token.Token
import io.github.numq.kortex.core.cache.LruCache
import io.github.numq.kortex.core.compose.font.EditorFont
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.paragraph.Paragraph
import org.jetbrains.skia.paragraph.ParagraphBuilder
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

        TextStyle().apply {
            setFontFamily(font.familyName)
            this.fontSize = font.size
            this.color = defaultColor
        }.use { baseTextStyle ->
            ParagraphStyle().apply {
                this.maxLinesCount = 1
                this.textStyle = baseTextStyle
            }.use { style ->
                fun ParagraphBuilder.addStyledSegment(segment: String, color: Int) {
                    if (segment.isEmpty()) return
                    TextStyle().apply {
                        this.fontFamilies = baseTextStyle.fontFamilies
                        this.fontSize = baseTextStyle.fontSize
                        this.color = color
                    }.use { segmentStyle ->
                        pushStyle(segmentStyle)
                        addText(segment)
                        popStyle()
                    }
                }

                font.buildParagraph(style) {
                    when {
                        tokens.isEmpty() -> addStyledSegment(text, defaultColor)

                        else -> {
                            var currentIndex = 0

                            val sortedTokens = tokens.filter { token ->
                                token.range.start.line <= line && token.range.end.line >= line
                            }.sortedWith(compareBy<Token> { token ->
                                if (token.range.start.line < line) 0 else token.range.start.column
                            }.thenBy { token ->
                                if (token.range.end.line > line) text.length else token.range.end.column
                            })

                            for ((range, type) in sortedTokens) {
                                val rawStart = when {
                                    range.start.line < line -> 0
                                    else -> range.start.column.coerceIn(0, text.length)
                                }

                                val start = rawStart.coerceAtLeast(currentIndex)

                                val end = when {
                                    range.end.line > line -> text.length
                                    else -> range.end.column.coerceIn(start, text.length)
                                }

                                if (start > currentIndex) {
                                    addStyledSegment(text.substring(currentIndex, start), defaultColor)
                                }

                                if (end > start) {
                                    val tokenColor =
                                        syntaxTheme.resolver.resolveColor(type, syntaxTheme.defaultText).toArgb()

                                    addStyledSegment(text.substring(start, end), tokenColor)
                                    currentIndex = end
                                }
                            }

                            if (currentIndex < text.length) {
                                addStyledSegment(text.substring(currentIndex), defaultColor)
                            }
                        }
                    }
                }.use { builder ->
                    builder.build().apply { layout(Float.MAX_VALUE) }
                }
            }
        }
    }
}