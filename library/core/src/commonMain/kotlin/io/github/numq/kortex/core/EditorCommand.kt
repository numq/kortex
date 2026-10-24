package io.github.numq.kortex.core

import io.github.numq.kortex.core.selection.Selection
import io.github.numq.krope.text.TextOperation

/**
 * Side effect commands produced by [EditorReducer] and executed by [EditorEngine].
 */
sealed interface EditorCommand {
    /** Applies a low-level text operation to the underlying rope buffer. */
    data class ApplyText(
        val operation: TextOperation.Data,
        val canMerge: Boolean,
        val targetSelections: List<Selection>? = null,
    ) : EditorCommand

    /** Reverts the previous operation. */
    data object Undo : EditorCommand

    /** Re-applies the next undone operation. */
    data object Redo : EditorCommand
}