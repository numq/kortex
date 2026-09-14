package io.github.numq.kortex.core.compose.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import io.github.numq.kortex.core.EditorAction
import io.github.numq.kortex.core.caret.CaretAction

internal object EditorInputMapper {
    fun mapToNavigationAction(event: KeyEvent): EditorAction? {
        val isShift = event.isShiftPressed

        val isCtrl = event.isPrimaryModifierPressed

        return when (event.key) {
            Key.DirectionLeft -> when {
                isCtrl -> CaretAction.MoveWordLeft(withSelection = isShift)

                else -> CaretAction.MoveLeft(withSelection = isShift)
            }

            Key.DirectionRight -> when {
                isCtrl -> CaretAction.MoveWordRight(withSelection = isShift)

                else -> CaretAction.MoveRight(withSelection = isShift)
            }

            Key.DirectionUp -> CaretAction.MoveUp(withSelection = isShift)

            Key.DirectionDown -> CaretAction.MoveDown(withSelection = isShift)

            Key.MoveHome -> when {
                isCtrl -> CaretAction.MoveDocStart(withSelection = isShift)

                else -> CaretAction.MoveLineStart(withSelection = isShift)
            }

            Key.MoveEnd -> when {
                isCtrl -> CaretAction.MoveDocEnd(withSelection = isShift)

                else -> CaretAction.MoveLineEnd(withSelection = isShift)
            }

            Key.PageUp -> CaretAction.MoveDocStart(withSelection = isShift)

            Key.PageDown -> CaretAction.MoveDocEnd(withSelection = isShift)

            else -> null
        }
    }
}