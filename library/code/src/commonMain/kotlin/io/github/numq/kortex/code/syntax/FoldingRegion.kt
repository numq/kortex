package io.github.numq.kortex.code.syntax

import io.github.numq.krope.text.TextRange

sealed interface FoldingRegion {
    val range: TextRange

    data class Expanded(override val range: TextRange) : FoldingRegion

    data class Collapsed(override val range: TextRange) : FoldingRegion
}