package io.github.numq.kortex.core.caret

import io.github.numq.kortex.core.EditorAction
import io.github.numq.krope.text.TextPosition

sealed interface CaretAction : EditorAction {
    data class Move(val position: TextPosition, val withSelection: Boolean = false) : CaretAction
    data class AddCursor(val position: TextPosition) : CaretAction
    data class MoveLeft(val withSelection: Boolean = false) : CaretAction
    data class MoveRight(val withSelection: Boolean = false) : CaretAction
    data class MoveUp(val withSelection: Boolean = false) : CaretAction
    data class MoveDown(val withSelection: Boolean = false) : CaretAction
    data class MoveWordLeft(val withSelection: Boolean = false) : CaretAction
    data class MoveWordRight(val withSelection: Boolean = false) : CaretAction
    data class MoveLineStart(val withSelection: Boolean = false) : CaretAction
    data class MoveLineEnd(val withSelection: Boolean = false) : CaretAction
    data class MoveDocStart(val withSelection: Boolean = false) : CaretAction
    data class MoveDocEnd(val withSelection: Boolean = false) : CaretAction
}