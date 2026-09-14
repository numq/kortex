package io.github.numq.kortex.core.compose.keymap

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import io.github.numq.kortex.core.compose.key.EditorKeyContext
import io.github.numq.kortex.core.compose.key.KeyBinding
import io.github.numq.kortex.core.compose.key.KeyStroke

class KeymapBuilder {
    private val bindings = mutableListOf<KeyBinding>()

    fun bind(
        stroke: KeyStroke,
        requiresEditable: Boolean = false,
        description: String? = null,
        handler: EditorKeyContext.() -> Unit,
    ) {
        bindings.add(
            KeyBinding(
                matcher = stroke::matches, requiresEditable = requiresEditable, description = description, handler = {
                    this.handler()

                    true
                })
        )
    }

    fun bind(
        key: Key,
        primary: Boolean = false,
        shift: Boolean = false,
        alt: Boolean = false,
        requiresEditable: Boolean = false,
        description: String? = null,
        handler: EditorKeyContext.() -> Unit,
    ) = bind(
        stroke = KeyStroke(key = key, primary = primary, shift = shift, alt = alt),
        requiresEditable = requiresEditable,
        description = description,
        handler = handler
    )

    fun bind(
        keys: Set<Key>,
        primary: Boolean = false,
        shift: Boolean = false,
        alt: Boolean = false,
        requiresEditable: Boolean = false,
        description: String? = null,
        handler: EditorKeyContext.() -> Unit,
    ) = bind(
        stroke = KeyStroke(keys = keys, primary = primary, shift = shift, alt = alt),
        requiresEditable = requiresEditable,
        description = description,
        handler = handler
    )

    fun bindCustom(
        matcher: (KeyEvent) -> Boolean,
        requiresEditable: Boolean = false,
        description: String? = null,
        handler: EditorKeyContext.() -> Unit,
    ) {
        bindings.add(
            KeyBinding(
                matcher = matcher, requiresEditable = requiresEditable, description = description, handler = {
                    this.handler()

                    true
                })
        )
    }

    fun bindConditional(
        matcher: KeyEvent.() -> Boolean,
        requiresEditable: Boolean = false,
        description: String? = null,
        handler: EditorKeyContext.() -> Boolean,
    ) {
        bindings.add(
            KeyBinding(
                matcher = matcher, requiresEditable = requiresEditable, description = description, handler = handler
            )
        )
    }

    fun include(keymap: EditorKeymap) {
        bindings.addAll(keymap.bindings)
    }

    fun build(): EditorKeymap = EditorKeymap(bindings.toList())
}

fun buildEditorKeymap(
    baseKeymap: EditorKeymap? = null,
    builder: KeymapBuilder.() -> Unit,
): EditorKeymap = KeymapBuilder().apply {
    builder()

    if (baseKeymap != null) {
        include(baseKeymap)
    }
}.build()