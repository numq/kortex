package io.github.numq.kortex.core.compose.keymap

import androidx.compose.ui.input.key.KeyEvent
import io.github.numq.kortex.core.compose.key.EditorKeyContext
import io.github.numq.kortex.core.compose.key.KeyBinding
import io.github.numq.kortex.core.compose.key.KeyStroke

class EditorKeymap(val bindings: List<KeyBinding>) {
    fun handle(event: KeyEvent, context: EditorKeyContext): Boolean {
        for (binding in bindings) {
            if (binding.execute(event, context)) {
                return true
            }
        }

        return false
    }

    operator fun plus(other: EditorKeymap): EditorKeymap = EditorKeymap(other.bindings + this.bindings)

    fun withBinding(
        stroke: KeyStroke,
        requiresEditable: Boolean = false,
        description: String? = null,
        handler: EditorKeyContext.() -> Unit,
    ): EditorKeymap = EditorKeymap(
        listOf(
            KeyBinding(
                matcher = stroke::matches, requiresEditable = requiresEditable, description = description, handler = {
                    this.handler()

                    true
                })
        ) + this.bindings
    )

    fun withConditionalBinding(
        stroke: KeyStroke,
        requiresEditable: Boolean = false,
        description: String? = null,
        handler: EditorKeyContext.() -> Boolean,
    ): EditorKeymap = EditorKeymap(
        listOf(
            KeyBinding(
                matcher = stroke::matches,
                requiresEditable = requiresEditable,
                description = description,
                handler = handler
            )
        ) + this.bindings
    )
}