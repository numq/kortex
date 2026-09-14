package io.github.numq.kortex.core.edit

import io.github.numq.kortex.core.EditorAction

sealed interface EditAction : EditorAction {
    data class Insert(val text: String) : EditAction

    data class Paste(val text: String) : EditAction

    data object Backspace : EditAction

    data object Delete : EditAction

    data object Enter : EditAction

    data object Tab : EditAction

    data object Untab : EditAction

    data object MoveLineUp : EditAction

    data object MoveLineDown : EditAction

    data object JoinLines : EditAction

    data object Duplicate : EditAction

    data object WordDeleteLeft : EditAction

    data object WordDeleteRight : EditAction
}