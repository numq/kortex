package io.github.numq.kortex.core.history

import io.github.numq.kortex.core.EditorCommand
import io.github.numq.kortex.core.EditorResult
import io.github.numq.kortex.core.EditorState

internal object HistoryReducer {
    fun reduce(state: EditorState, action: HistoryAction) = when (action) {
        is HistoryAction.Undo -> EditorResult(state, EditorCommand.Undo)

        is HistoryAction.Redo -> EditorResult(state, EditorCommand.Redo)
    }
}