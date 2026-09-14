package io.github.numq.kortex.core

import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.caret.CaretReducer
import io.github.numq.kortex.core.edit.EditAction
import io.github.numq.kortex.core.edit.EditReducer
import io.github.numq.kortex.core.history.HistoryAction
import io.github.numq.kortex.core.history.HistoryReducer
import io.github.numq.kortex.core.selection.SelectionAction
import io.github.numq.kortex.core.selection.SelectionReducer
import io.github.numq.kortex.core.viewport.ViewportAction
import io.github.numq.kortex.core.viewport.ViewportReducer

object EditorReducer {
    fun reduce(state: EditorState, action: EditorAction) = when (action) {
        is EditAction -> EditReducer.reduce(state, action)

        is CaretAction -> CaretReducer.reduce(state, action)

        is SelectionAction -> SelectionReducer.reduce(state, action)

        is ViewportAction -> ViewportReducer.reduce(state, action)

        is HistoryAction -> HistoryReducer.reduce(state, action)

        is EditorAction.UpdateConfig -> EditorResult(state.copy(config = action.config))

        else -> EditorResult(state)
    }
}