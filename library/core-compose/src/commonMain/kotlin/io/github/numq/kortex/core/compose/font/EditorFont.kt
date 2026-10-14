package io.github.numq.kortex.core.compose.font

import io.github.numq.kortex.core.lifecycle.CloseableResource
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.paragraph.*

class EditorFont(val typeface: Typeface, val size: Float, val lineSpacing: Float) : CloseableResource() {
    private val fontProvider = TypefaceFontProvider().apply {
        registerTypeface(typeface)
    }

    private val collection = FontCollection().apply {
        setAssetFontManager(fontProvider)

        setDefaultFontManager(FontMgr.default)
    }

    private val skiaFont = Font(typeface = typeface, size = size).apply {
        this.edging = FontEdging.SUBPIXEL_ANTI_ALIAS

        this.hinting = FontHinting.SLIGHT

        this.isLinearMetrics = true

        this.isSubpixel = true

        setBitmapsEmbedded(true)
    }

    private val metrics = skiaFont.metrics

    val ascent = metrics.ascent

    val descent = metrics.descent

    val charWidth = skiaFont.getWidths(skiaFont.getStringGlyphs(" ")).firstOrNull() ?: size

    val textHeight = descent - ascent

    val lineHeight = textHeight * lineSpacing

    val familyName = typeface.familyName

    fun getOffsetAtCoordinate(text: String, x: Float): Int {
        if (x <= 0f || text.isEmpty()) return 0

        checkOpen()

        val offset = TextStyle().apply {
            this.fontFamilies = arrayOf(this@EditorFont.familyName)
            this.fontSize = this@EditorFont.size
        }.use { baseTextStyle ->
            ParagraphStyle().apply {
                this.maxLinesCount = 1
                this.textStyle = baseTextStyle
            }.use { style ->
                buildParagraph(style) {
                    addText(text)
                }.use { builder ->
                    builder.build().use { paragraph ->
                        paragraph.layout(Float.MAX_VALUE)
                        paragraph.getGlyphPositionAtCoordinate(x, 0f).position
                    }
                }
            }
        }

        return offset.coerceIn(0, text.length)
    }

    fun createTextLine(text: String): TextLine {
        checkOpen()

        return TextLine.make(text, skiaFont)
    }

    fun buildParagraph(style: ParagraphStyle, builder: ParagraphBuilder.() -> Unit): ParagraphBuilder {
        checkOpen()

        return ParagraphBuilder(style = style, fc = collection).apply(builder)
    }

    fun measureTextWidth(text: String, paint: Paint? = null): Float {
        checkOpen()

        return skiaFont.measureTextWidth(text, paint)
    }

    override fun onRelease() {
        skiaFont.close()

        collection.close()

        fontProvider.close()
    }
}