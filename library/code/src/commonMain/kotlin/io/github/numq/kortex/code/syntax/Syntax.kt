package io.github.numq.kortex.code.syntax

import io.github.numq.kortex.code.token.Token
import io.github.numq.krope.text.TextRevision

data class Syntax(
    val revision: TextRevision,
    val foldingRegions: List<FoldingRegion>,
    val occurrences: List<Occurrence>,
    val tokensPerLine: Map<Int, List<Token>>,
)