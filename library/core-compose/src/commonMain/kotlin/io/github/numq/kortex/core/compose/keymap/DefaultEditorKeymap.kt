package io.github.numq.kortex.core.compose.keymap

import androidx.compose.ui.input.key.Key
import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.edit.EditAction
import io.github.numq.kortex.core.history.HistoryAction
import io.github.numq.kortex.core.selection.SelectionAction

object DefaultEditorKeymap {
    fun create(): EditorKeymap = buildEditorKeymap {
        bind(Key.Escape, description = "Clear Selection") {
            dispatch(SelectionAction.Clear)
        }

        bind(Key.A, primary = true, description = "Select All") {
            dispatch(SelectionAction.SelectAll)
        }

        bind(Key.D, primary = true, description = "Select Next Match") {
            dispatch(SelectionAction.SelectNextMatch)
        }

        bind(Key.Z, primary = true, shift = false, requiresEditable = true, description = "Undo") {
            dispatch(HistoryAction.Undo)
        }

        bind(Key.Z, primary = true, shift = true, requiresEditable = true, description = "Redo") {
            dispatch(HistoryAction.Redo)
        }

        bind(Key.C, primary = true, description = "Copy") { copy() }

        bind(Key.X, primary = true, requiresEditable = true, description = "Cut") { cut() }

        bind(Key.V, primary = true, requiresEditable = true, description = "Paste") { paste() }

        bind(Key.D, primary = true, shift = true, requiresEditable = true, description = "Duplicate Line/Selection") {
            dispatch(EditAction.Duplicate)
        }

        bind(Key.Backspace, requiresEditable = true, description = "Backspace") {
            dispatch(EditAction.Backspace)
        }

        bind(Key.Backspace, primary = true, requiresEditable = true, description = "Delete Word Left") {
            dispatch(EditAction.WordDeleteLeft)
        }

        bind(Key.Delete, requiresEditable = true, description = "Delete") {
            dispatch(EditAction.Delete)
        }

        bind(Key.Delete, primary = true, requiresEditable = true, description = "Delete Word Right") {
            dispatch(EditAction.WordDeleteRight)
        }

        bind(setOf(Key.Enter, Key.NumPadEnter), requiresEditable = true, description = "New Line") {
            dispatch(EditAction.Enter)
        }

        bind(Key.Tab, shift = false, requiresEditable = true, description = "Indent / Tab") {
            dispatch(EditAction.Tab)
        }

        bind(Key.Tab, shift = true, requiresEditable = true, description = "Unindent / Untab") {
            dispatch(EditAction.Untab)
        }

        bind(Key.DirectionUp, alt = true, requiresEditable = true, description = "Move Line Up") {
            dispatch(EditAction.MoveLineUp)
        }

        bind(Key.DirectionDown, alt = true, requiresEditable = true, description = "Move Line Down") {
            dispatch(EditAction.MoveLineDown)
        }

        bind(
            Key.DirectionLeft, primary = false, shift = false
        ) {
            dispatch(CaretAction.MoveLeft(withSelection = false))
        }

        bind(Key.DirectionLeft, primary = false, shift = true) {
            dispatch(CaretAction.MoveLeft(withSelection = true))
        }

        bind(
            Key.DirectionLeft, primary = true, shift = false
        ) {
            dispatch(CaretAction.MoveWordLeft(withSelection = false))
        }

        bind(
            Key.DirectionLeft, primary = true, shift = true
        ) {
            dispatch(CaretAction.MoveWordLeft(withSelection = true))
        }

        bind(
            Key.DirectionRight, primary = false, shift = false
        ) {
            dispatch(CaretAction.MoveRight(withSelection = false))
        }

        bind(
            Key.DirectionRight, primary = false, shift = true
        ) {
            dispatch(CaretAction.MoveRight(withSelection = true))
        }

        bind(
            Key.DirectionRight, primary = true, shift = false
        ) {
            dispatch(CaretAction.MoveWordRight(withSelection = false))
        }

        bind(
            Key.DirectionRight, primary = true, shift = true
        ) {
            dispatch(CaretAction.MoveWordRight(withSelection = true))
        }

        bind(
            Key.DirectionUp, primary = false, shift = false, alt = false
        ) {
            dispatch(CaretAction.MoveUp(withSelection = false))
        }

        bind(
            Key.DirectionUp, primary = false, shift = true, alt = false
        ) {
            dispatch(CaretAction.MoveUp(withSelection = true))
        }

        bind(Key.DirectionDown, primary = false, shift = false, alt = false) {
            dispatch(CaretAction.MoveDown(withSelection = false))
        }

        bind(Key.DirectionDown, primary = false, shift = true, alt = false) {
            dispatch(CaretAction.MoveDown(withSelection = true))
        }

        bind(
            Key.MoveHome, primary = false, shift = false
        ) {
            dispatch(CaretAction.MoveLineStart(withSelection = false))
        }

        bind(Key.MoveHome, primary = false, shift = true) {
            dispatch(CaretAction.MoveLineStart(withSelection = true))
        }

        bind(Key.MoveHome, primary = true, shift = false) {
            dispatch(CaretAction.MoveDocStart(withSelection = false))
        }

        bind(Key.MoveHome, primary = true, shift = true) {
            dispatch(CaretAction.MoveDocStart(withSelection = true))
        }

        bind(Key.MoveEnd, primary = false, shift = false) {
            dispatch(CaretAction.MoveLineEnd(withSelection = false))
        }

        bind(Key.MoveEnd, primary = false, shift = true) {
            dispatch(CaretAction.MoveLineEnd(withSelection = true))
        }

        bind(Key.MoveEnd, primary = true, shift = false) {
            dispatch(CaretAction.MoveDocEnd(withSelection = false))
        }

        bind(Key.MoveEnd, primary = true, shift = true) {
            dispatch(CaretAction.MoveDocEnd(withSelection = true))
        }

        bind(Key.PageUp, shift = false) {
            scrollPage(up = true)

            dispatch(CaretAction.MoveDocStart(withSelection = false))
        }

        bind(Key.PageUp, shift = true) {
            scrollPage(up = true)

            dispatch(CaretAction.MoveDocStart(withSelection = true))
        }

        bind(Key.PageDown, shift = false) {
            scrollPage(up = false)

            dispatch(CaretAction.MoveDocEnd(withSelection = false))
        }

        bind(Key.PageDown, shift = true) {
            scrollPage(up = false)

            dispatch(CaretAction.MoveDocEnd(withSelection = true))
        }
    }
}