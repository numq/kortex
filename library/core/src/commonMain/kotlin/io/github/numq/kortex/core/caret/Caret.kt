package io.github.numq.kortex.core.caret

import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextSnapshot

/**
 * Represents the position of a single cursor.
 *
 * @property position 0-based line and column coordinates of the cursor.
 */
data class Caret(val position: TextPosition) {
    /** Clamps the caret coordinates within the valid bounds of [snapshot]. */
    fun coerceIn(snapshot: TextSnapshot): Caret {
        val coercedPosition = position.coerceIn(snapshot)

        return when (position) {
            coercedPosition -> this

            else -> copy(position = coercedPosition)
        }
    }

    companion object {
        /** Caret located at line 0, column 0. */
        val ZERO = Caret(position = TextPosition.ZERO)
    }
}