package io.github.numq.kortex.code

import io.github.numq.kortex.core.EditorCommand
import io.github.numq.krope.text.TextOperation

/**
 * Side effect commands produced by [CodeEditorReducer].
 */
sealed interface CodeEditorCommand {
    /** Delegates a command to the underlying core editor engine. */
    data class Core(val command: EditorCommand) : CodeEditorCommand

    /**
     * Applies a text operation to the underlying document buffer.
     *
     * @property operation The operation to apply.
     * @property canMerge Whether this operation can merge with previous history entries.
     */
    data class ApplyText(val operation: TextOperation.Data, val canMerge: Boolean = false) : CodeEditorCommand
}