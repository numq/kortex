package io.github.numq.kortex.core

import io.github.numq.krope.text.TextOperation

sealed interface EditorCommand {
    data class ApplyText(val operation: TextOperation.Data, val canMerge: Boolean) : EditorCommand

    data object Undo : EditorCommand

    data object Redo : EditorCommand
}