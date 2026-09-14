package io.github.numq.kortex.core.compose.layer

import io.github.numq.kortex.core.caret.Caret
import io.github.numq.kortex.core.compose.background.BackgroundLayer
import io.github.numq.kortex.core.compose.background.HighlightedLineLayer
import io.github.numq.kortex.core.compose.caret.CaretLayer
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.gutter.GutterLineLayer
import io.github.numq.kortex.core.compose.gutter.GutterSeparatorLayer
import io.github.numq.kortex.core.compose.selection.SelectionLayer
import io.github.numq.kortex.core.compose.text.TextContentLayer
import io.github.numq.kortex.core.compose.theme.EditorTheme
import io.github.numq.kortex.core.lifecycle.ManagedResource
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.viewport.ViewportLine

interface LayerFactory : ManagedResource {
    fun createBackgroundLayer(width: Float, height: Float, theme: EditorTheme): BackgroundLayer

    fun createHighlightedLineLayer(
        viewportLines: List<ViewportLine>,
        caret: Caret,
        theme: EditorTheme,
    ): HighlightedLineLayer?

    fun createGutterLineLayer(
        line: Int,
        width: Float,
        textY: Float,
        font: EditorFont,
        theme: EditorTheme,
    ): GutterLineLayer

    fun createGutterSeparatorLayer(x: Float, height: Float, theme: EditorTheme): GutterSeparatorLayer

    fun createCodeAreaContentLayers(
        viewportLines: List<ViewportLine>,
        scrollX: Float,
        font: EditorFont,
        theme: EditorTheme,
    ): List<TextContentLayer>

    fun createSelectionLayer(
        contentLayers: List<TextContentLayer>,
        selection: Selection,
        scrollX: Float,
        theme: EditorTheme,
    ): SelectionLayer

    fun createCaretLayers(
        contentLayers: List<TextContentLayer>,
        selections: List<Selection>,
        scrollX: Float,
        font: EditorFont,
        theme: EditorTheme,
    ): List<CaretLayer>
}