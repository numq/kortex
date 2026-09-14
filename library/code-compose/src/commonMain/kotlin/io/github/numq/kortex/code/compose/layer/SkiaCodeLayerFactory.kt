package io.github.numq.kortex.code.compose.layer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import io.github.numq.kortex.code.analysis.CodeIssue
import io.github.numq.kortex.code.compose.cache.CodeParagraphCache
import io.github.numq.kortex.code.compose.diagnostics.DiagnosticsLayer
import io.github.numq.kortex.code.compose.gutter.FoldingMarkersLayer
import io.github.numq.kortex.code.compose.occurrences.OccurrencesHighlightLayer
import io.github.numq.kortex.code.compose.ruler.ColumnRulerLayer
import io.github.numq.kortex.code.compose.theme.CodeEditorTheme
import io.github.numq.kortex.code.syntax.FoldingRegion
import io.github.numq.kortex.code.syntax.Occurrence
import io.github.numq.kortex.code.token.Token
import io.github.numq.kortex.core.caret.Caret
import io.github.numq.kortex.core.compose.cache.PaintCache
import io.github.numq.kortex.core.compose.cache.ParagraphCache
import io.github.numq.kortex.core.compose.cache.TextLineCache
import io.github.numq.kortex.core.compose.dimensions.EditorDimensions
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.layer.LayerFactory
import io.github.numq.kortex.core.compose.layer.SkiaLayerFactory
import io.github.numq.kortex.core.compose.text.TextContentLayer
import io.github.numq.kortex.core.viewport.ViewportLine
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Rect

internal class SkiaCodeLayerFactory(
    private val baseFactory: LayerFactory,
    private val paintCache: PaintCache,
    private val codeParagraphCache: CodeParagraphCache,
    private val dimensions: EditorDimensions,
) : CodeLayerFactory, LayerFactory by baseFactory {
    override fun createFoldingMarkersLayers(
        viewportLines: List<ViewportLine>,
        gutterWidth: Float,
        foldingRegions: List<FoldingRegion>?,
        collapsedLines: Set<Int>,
        theme: CodeEditorTheme,
    ): List<FoldingMarkersLayer> {
        val regions = foldingRegions.orEmpty()

        val linesWithFolding = regions.map { region ->
            region.range.start.line
        }.toSet()

        val paint = paintCache.getOrCreate(
            PaintCache.Key(color = theme.folding.iconColor.toArgb(), mode = PaintMode.FILL)
        ).getOrThrow()

        return viewportLines.filter { viewportLine ->
            viewportLine.line in linesWithFolding
        }.map { viewportLine ->
            val size = viewportLine.height * .7f

            val x = gutterWidth - size - dimensions.gutterPaddingEnd

            val y = viewportLine.y + (viewportLine.height - size) / 2f

            FoldingMarkersLayer(
                line = viewportLine.line,
                rect = Rect.makeXYWH(x, y, size, size),
                isCollapsed = viewportLine.line in collapsedLines,
                paint = paint
            )
        }
    }

    override fun createColumnRulerLayer(
        column: Int,
        height: Float,
        scrollX: Float,
        font: EditorFont,
        theme: CodeEditorTheme,
    ): ColumnRulerLayer {
        val paint = paintCache.getOrCreate(
            PaintCache.Key(color = theme.ruler.toArgb())
        ).getOrThrow()

        val x = (column * font.charWidth) - scrollX + dimensions.editorPaddingStart

        return ColumnRulerLayer(x = x, height = height, paint = paint)
    }

    override fun createSyntaxContentLayers(
        viewportLines: List<ViewportLine>,
        tokensPerLine: Map<Int, List<Token>>?,
        scrollX: Float,
        font: EditorFont,
        theme: CodeEditorTheme,
    ) = viewportLines.map { viewportLine ->
        val tokens = tokensPerLine?.get(viewportLine.line).orEmpty()

        val paragraph = codeParagraphCache.getOrCreate(
            CodeParagraphCache.Key(
                line = viewportLine.line,
                text = viewportLine.text,
                tokens = tokens,
                font = font,
                syntaxTheme = theme.syntax
            )
        ).getOrThrow()

        TextContentLayer(
            viewportLine = viewportLine,
            paragraph = paragraph,
            x = -scrollX + dimensions.editorPaddingStart,
            y = viewportLine.textBaselineY + font.ascent
        )
    }

    override fun createOccurrencesLayers(
        contentLayers: List<TextContentLayer>,
        occurrences: List<Occurrence>,
        caret: Caret,
        scrollX: Float,
        theme: CodeEditorTheme,
    ): List<OccurrencesHighlightLayer> {
        val matchPaint = paintCache.getOrCreate(
            PaintCache.Key(color = theme.occurrences.matchBackground.toArgb())
        ).getOrThrow()

        val currentPaint = paintCache.getOrCreate(
            PaintCache.Key(color = theme.occurrences.currentMatchBackground.toArgb())
        ).getOrThrow()

        return contentLayers.flatMap { layer ->
            occurrences.filter { occurrence ->
                occurrence.range.start.line == layer.viewportLine.line
            }.map { occurrence ->
                val startX = layer.getCoordinateAtOffset(occurrence.range.start.column)

                val endX = layer.getCoordinateAtOffset(occurrence.range.end.column)

                val x = -scrollX + startX + dimensions.editorPaddingStart

                val paint = when {
                    occurrence.range.contains(caret.position) -> currentPaint

                    else -> matchPaint
                }

                OccurrencesHighlightLayer(
                    rect = Rect.makeXYWH(x, layer.viewportLine.y, endX - startX, layer.viewportLine.height),
                    paint = paint
                )
            }
        }
    }

    override fun createDiagnosticsLayers(
        contentLayers: List<TextContentLayer>,
        issues: List<CodeIssue>,
        scrollX: Float,
        theme: CodeEditorTheme,
    ): List<DiagnosticsLayer> = contentLayers.flatMap { layer ->
        issues.filter { issue ->
            issue.range.start.line == layer.viewportLine.line
        }.map { issue ->
            val startX = layer.getCoordinateAtOffset(issue.range.start.column)

            val endX = layer.getCoordinateAtOffset(issue.range.end.column)

            val color = when (issue) {
                is CodeIssue.Error -> theme.diagnostics.error

                is CodeIssue.Warning -> theme.diagnostics.warning

                is CodeIssue.Information -> theme.diagnostics.info

                is CodeIssue.Hint -> theme.diagnostics.hint

                is CodeIssue.Unknown -> theme.diagnostics.hint
            }

            val paint = paintCache.getOrCreate(
                PaintCache.Key(color = color.toArgb(), mode = PaintMode.STROKE, strokeWidth = 1.5f)
            ).getOrThrow()

            DiagnosticsLayer(
                startX = startX - scrollX + dimensions.editorPaddingStart,
                endX = endX - scrollX + dimensions.editorPaddingStart,
                baselineY = layer.viewportLine.textBaselineY,
                paint = paint
            )
        }
    }

    override fun close() {
        baseFactory.close()

        paintCache.close()

        codeParagraphCache.close()
    }
}

@Composable
fun rememberCodeLayerFactory(dimensions: EditorDimensions): CodeLayerFactory {
    val factory = remember(dimensions) {
        val paintCache = PaintCache(1000)

        val textLineCache = TextLineCache(1000)

        val codeParagraphCache = CodeParagraphCache(1000)

        val baseFactory = SkiaLayerFactory(
            textLineCache = textLineCache,
            paintCache = paintCache,
            paragraphCache = ParagraphCache(1),
            dimensions = dimensions,
        )

        SkiaCodeLayerFactory(
            baseFactory = baseFactory,
            paintCache = paintCache,
            codeParagraphCache = codeParagraphCache,
            dimensions = dimensions,
        )
    }

    DisposableEffect(factory) {
        onDispose {
            factory.close()
        }
    }

    return factory
}