package io.github.numq.kortex.core.compose.key

import androidx.compose.ui.input.key.KeyEvent

class KeyBinding(
    val matcher: (KeyEvent) -> Boolean,
    val requiresEditable: Boolean = false,
    val description: String? = null,
    val handler: EditorKeyContext.() -> Boolean,
) {
    fun execute(event: KeyEvent, context: EditorKeyContext): Boolean {
        if (!matcher(event)) return false

        if (requiresEditable && context.isReadOnly) {
            return true
        }

        return context.handler()
    }
}