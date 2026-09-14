package io.github.numq.kortex.core.compose.key

import androidx.compose.ui.input.key.*
import io.github.numq.kortex.core.compose.input.isPrimaryModifierPressed

data class KeyStroke(
    val keys: Set<Key>,
    val primary: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
) {
    constructor(
        key: Key,
        primary: Boolean = false,
        shift: Boolean = false,
        alt: Boolean = false,
    ) : this(keys = setOf(key), primary = primary, shift = shift, alt = alt)

    fun matches(event: KeyEvent): Boolean {
        if (event.key !in keys) return false

        val eventPrimary = event.isPrimaryModifierPressed

        val eventShift = event.isShiftPressed

        val eventAlt = event.isAltPressed

        return eventPrimary == primary && eventShift == shift && eventAlt == alt
    }

    companion object {
        fun of(keys: Set<Key>, primary: Boolean = false, shift: Boolean = false, alt: Boolean = false) =
            KeyStroke(keys = keys.toSet(), primary = primary, shift = shift, alt = alt)
    }
}