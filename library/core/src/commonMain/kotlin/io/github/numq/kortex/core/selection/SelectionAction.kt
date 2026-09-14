package io.github.numq.kortex.core.selection

import io.github.numq.kortex.core.EditorAction
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange

sealed interface SelectionAction : EditorAction {
    data object Clear : SelectionAction

    data object SelectAll : SelectionAction

    data class SelectWordAt(val position: TextPosition) : SelectionAction

    data class SelectLine(val line: Int) : SelectionAction

    data class SelectRange(val range: TextRange) : SelectionAction

    data class AddSelection(val selection: Selection) : SelectionAction

    data class SetSelections(val selections: List<Selection>) : SelectionAction

    data object SelectNextMatch : SelectionAction
}