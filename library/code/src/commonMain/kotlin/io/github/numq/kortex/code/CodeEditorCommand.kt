package io.github.numq.kortex.code

import io.github.numq.kortex.core.EditorCommand
import io.github.numq.krope.text.TextOperation

sealed interface CodeEditorCommand {
    data class Core(val command: EditorCommand) : CodeEditorCommand

    data class ApplyText(val operation: TextOperation.Data, val canMerge: Boolean = false) : CodeEditorCommand
}