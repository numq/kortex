package io.github.numq.kortex.code.syntax

import io.github.numq.krope.text.TextRange

/**
 * A foldable block of text across multiple lines.
 */
sealed interface FoldingRegion {
    /** Document range spanned by the foldable block. */
    val range: TextRange

    /** A foldable region that is currently expanded. */
    data class Expanded(override val range: TextRange) : FoldingRegion

    /** A foldable region that is currently collapsed. */
    data class Collapsed(override val range: TextRange) : FoldingRegion
}