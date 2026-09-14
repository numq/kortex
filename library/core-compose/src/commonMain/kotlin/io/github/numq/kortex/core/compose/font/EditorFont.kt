package io.github.numq.kortex.core.compose.font

import io.github.numq.kortex.core.lifecycle.CloseableResource
import org.jetbrains.skia.*
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.ParagraphBuilder
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.TypefaceFontProvider

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