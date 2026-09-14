package io.github.numq.kortex.code.compose.layer

import io.github.numq.kortex.code.analysis.CodeIssue
import io.github.numq.kortex.code.compose.diagnostics.DiagnosticsLayer
import io.github.numq.kortex.code.compose.gutter.FoldingMarkersLayer
import io.github.numq.kortex.code.compose.occurrences.OccurrencesHighlightLayer
import io.github.numq.kortex.code.compose.ruler.ColumnRulerLayer
import io.github.numq.kortex.code.compose.theme.CodeEditorTheme
import io.github.numq.kortex.code.syntax.FoldingRegion
import io.github.numq.kortex.code.syntax.Occurrence
import io.github.numq.kortex.code.token.Token
import io.github.numq.kortex.core.caret.Caret
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.layer.LayerFactory
import io.github.numq.kortex.core.compose.text.TextContentLayer
import io.github.numq.kortex.core.viewport.ViewportLine

interface CodeLayerFactory : LayerFactory {
    fun createFoldingMarkersLayers(
        viewportLines: List<ViewportLine>,
        gutterWidth: Float,
        foldingRegions: List<FoldingRegion>?,
        collapsedLines: Set<Int>,
        theme: CodeEditorTheme,
    ): List<FoldingMarkersLayer>

    fun createColumnRulerLayer(
        column: Int,
        height: Float,
        scrollX: Float,
        font: EditorFont,
        theme: CodeEditorTheme,
    ): ColumnRulerLayer

    fun createSyntaxContentLayers(
        viewportLines: List<ViewportLine>,
        tokensPerLine: Map<Int, List<Token>>?,
        scrollX: Float,
        font: EditorFont,
        theme: CodeEditorTheme,
    ): List<TextContentLayer>

    fun createOccurrencesLayers(
        contentLayers: List<TextContentLayer>,
        occurrences: List<Occurrence>,
        caret: Caret,
        scrollX: Float,
        theme: CodeEditorTheme,
    ): List<OccurrencesHighlightLayer>

    fun createDiagnosticsLayers(
        contentLayers: List<TextContentLayer>,
        issues: List<CodeIssue>,
        scrollX: Float,
        theme: CodeEditorTheme,
    ): List<DiagnosticsLayer>
}