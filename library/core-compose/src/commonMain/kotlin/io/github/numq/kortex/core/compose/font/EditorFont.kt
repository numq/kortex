package io.github.numq.kortex.core.compose.font

import io.github.numq.kortex.core.cache.LruCache
import io.github.numq.kortex.core.lifecycle.CloseableResource
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.paragraph.*

/**
 * Monospace font wrapper backed by Skia Font and Paragraph subsystems.
 *
 * @property typeface The Skia [Typeface].
 * @property size Font size in points.
 * @property lineSpacing Line height multiplier (e.g. 1.2f).
 */
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

    private val measurementParagraphCache = object : LruCache<String, Paragraph>(capacity = 128) {
        override val factory: String.() -> Paragraph = {
            val textToMeasure = this
            TextStyle().apply {
                this.fontFamilies = arrayOf(this@EditorFont.familyName)
                this.fontSize = this@EditorFont.size
            }.use { baseTextStyle ->
                ParagraphStyle().apply {
                    this.maxLinesCount = 1
                    this.textStyle = baseTextStyle
                }.use { style ->
                    buildParagraph(style) {
                        pushStyle(baseTextStyle)
                        addText(textToMeasure)
                        popStyle()
                    }.use { builder ->
                        builder.build().apply {
                            layout(Float.MAX_VALUE)
                        }
                    }
                }
            }
        }
    }

    fun getOffsetAtCoordinate(text: String, x: Float): Int {
        if (x <= 0f || text.isEmpty()) return 0

        checkOpen()

        val paragraph = measurementParagraphCache.getOrCreate(text).getOrThrow()
        val offset = paragraph.getGlyphPositionAtCoordinate(x, 0f).position

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
        measurementParagraphCache.close()

        skiaFont.close()

        collection.close()

        fontProvider.close()
    }
}