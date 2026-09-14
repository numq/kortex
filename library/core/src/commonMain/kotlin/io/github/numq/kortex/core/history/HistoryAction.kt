package io.github.numq.kortex.core.history

import io.github.numq.kortex.core.EditorAction

sealed interface HistoryAction : EditorAction {
    data object Undo : HistoryAction

    data object Redo : HistoryAction
}