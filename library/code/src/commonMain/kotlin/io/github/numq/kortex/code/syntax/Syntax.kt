package io.github.numq.kortex.code.syntax

import io.github.numq.kortex.code.token.Token
import io.github.numq.krope.text.TextRevision

/**
 * Aggregated syntactic metadata for a specific document revision.
 *
 * @property revision The document revision for which this syntactic data was computed.
 * @property foldingRegions List of code folding blocks (functions, classes, blocks).
 * @property occurrences List of semantic symbol references/definitions matching the current symbol.
 * @property tokensPerLine Pre-grouped tokens keyed by their 0-based document line index.
 */
data class Syntax(
    val revision: TextRevision,
    val foldingRegions: List<FoldingRegion>,
    val occurrences: List<Occurrence>,
    val tokensPerLine: Map<Int, List<Token>>,
)