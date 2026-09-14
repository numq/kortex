package io.github.numq.kortex.core.caret

import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextSnapshot

data class Caret(val position: TextPosition) {
    fun coerceIn(snapshot: TextSnapshot): Caret {
        val coercedPosition = position.coerceIn(snapshot)

        return when (position) {
            coercedPosition -> this

            else -> copy(position = coercedPosition)
        }
    }

    companion object {
        val ZERO = Caret(position = TextPosition.ZERO)
    }
}