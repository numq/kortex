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
                    if (tokens.isEmpty() || text.isEmpty()) {
                        addStyledSegment(text, defaultColor)
                    } else {
                        val textLength = text.length

                        val lineTokens = tokens.filter { token ->
                            token.range.start.line <= line && token.range.end.line >= line
                        }.mapNotNull { token ->
                            val startCol = if (token.range.start.line < line) 0 else token.range.start.column
                            val endCol = if (token.range.end.line > line) textLength else token.range.end.column

                            val clampedStart = startCol.coerceIn(0, textLength)
                            val clampedEnd = endCol.coerceIn(clampedStart, textLength)

                            if (clampedStart < clampedEnd) {
                                Triple(clampedStart, clampedEnd, token.type)
                            } else null
                        }.sortedBy { it.first }

                        var currentIndex = 0

                        for ((start, end, type) in lineTokens) {
                            if (start < currentIndex) {
                                if (end <= currentIndex) continue
                                val adjustedStart = currentIndex
                                val tokenColor =
                                    syntaxTheme.resolver.resolveColor(type, syntaxTheme.defaultText).toArgb()
                                addStyledSegment(text.substring(adjustedStart, end), tokenColor)
                                currentIndex = end
                                continue
                            }

                            if (start > currentIndex) {
                                addStyledSegment(text.substring(currentIndex, start), defaultColor)
                                currentIndex = start
                            }

                            if (end > currentIndex) {
                                val tokenColor =
                                    syntaxTheme.resolver.resolveColor(type, syntaxTheme.defaultText).toArgb()
                                addStyledSegment(text.substring(currentIndex, end), tokenColor)
                                currentIndex = end
                            }
                        }

                        if (currentIndex < textLength) {
                            addStyledSegment(text.substring(currentIndex, textLength), defaultColor)
                        }
                    }
                }.use { builder ->
                    builder.build().apply { layout(Float.MAX_VALUE) }
                }
            }
        }
    }
}