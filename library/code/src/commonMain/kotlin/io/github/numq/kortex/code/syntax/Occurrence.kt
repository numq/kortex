package io.github.numq.kortex.code.syntax

import io.github.numq.krope.text.TextRange

/**
 * Represents an occurrence of a symbol in the document (used for highlight-under-caret).
 */
sealed interface Occurrence {
    /** Text coordinates of the occurrence. */
    val range: TextRange

    /** Occurrence that represents the declaration/definition site of the symbol. */
    data class Definition(override val range: TextRange) : Occurrence

    /** Occurrence that represents a call or reference site of the symbol. */
    data class Reference(override val range: TextRange) : Occurrence
}