package io.github.numq.kortex.code.syntax

import io.github.numq.krope.text.TextRange

sealed interface Occurrence {
    val range: TextRange

    data class Definition(override val range: TextRange) : Occurrence

    data class Reference(override val range: TextRange) : Occurrence
}