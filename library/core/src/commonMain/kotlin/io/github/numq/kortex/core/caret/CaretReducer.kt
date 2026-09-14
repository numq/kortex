package io.github.numq.kortex.core.caret

import io.github.numq.kortex.core.EditorResult
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.viewport.FoldMap
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextSnapshot

internal object CaretReducer {
    private fun moveSelectionLeft(
        selection: Selection, snapshot: TextSnapshot, foldMap: FoldMap, withSelection: Boolean
    ): Selection {
        if (!withSelection && selection.isNotEmpty) {
            return Selection.cursor(selection.range.start)
        }

        val current = selection.caret

        val next = when {
            current.column > 0 -> {
                val lineText = snapshot.getLineText(current.line)

                val step = when {
                    current.column >= 2 && lineText[current.column - 1].isLowSurrogate() && lineText[current.column - 2].isHighSurrogate() -> 2

                    else -> 1
                }

                current.copy(column = current.column - step)
            }

            current.line > 0 -> {
                val currentVisual = foldMap.documentToVisualLine(current.line)

                when {
                    currentVisual > 0 -> {
                        val prevDocLine = foldMap.visualToDocumentLine(currentVisual - 1)

                        TextPosition(line = prevDocLine, column = snapshot.getLineLength(prevDocLine))
                    }

                    else -> current
                }
            }

            else -> current
        }

        return selection.withCaret(newCaret = next, keepAnchor = withSelection, stickyCol = next.column)
    }

    private fun moveSelectionRight(
        selection: Selection, snapshot: TextSnapshot, foldMap: FoldMap, withSelection: Boolean
    ): Selection {
        if (!withSelection && selection.isNotEmpty) {
            return Selection.cursor(selection.range.end)
        }

        val current = selection.caret

        val currentLineLength = snapshot.getLineLength(current.line)

        val next = when {
            current.column < currentLineLength -> {
                val lineText = snapshot.getLineText(current.line)

                val step = when {
                    current.column + 1 < currentLineLength && lineText[current.column].isHighSurrogate() && lineText[current.column + 1].isLowSurrogate() -> 2

                    else -> 1
                }

                current.copy(column = current.column + step)
            }

            current.line < snapshot.lines - 1 -> {
                val currentVisual = foldMap.documentToVisualLine(current.line)

                when {
                    currentVisual < foldMap.totalVisualLines - 1 -> {
                        val nextDocLine = foldMap.visualToDocumentLine(currentVisual + 1)

                        TextPosition(line = nextDocLine, column = 0)
                    }

                    else -> current
                }
            }

            else -> current
        }

        return selection.withCaret(newCaret = next, keepAnchor = withSelection, stickyCol = next.column)
    }

    private fun moveSelectionUp(
        selection: Selection, snapshot: TextSnapshot, foldMap: FoldMap, withSelection: Boolean
    ): Selection {
        val currentVisual = foldMap.documentToVisualLine(selection.caret.line)

        if (currentVisual <= 0) return selection

        val targetLine = foldMap.visualToDocumentLine(currentVisual - 1)

        val targetColumn = selection.stickyColumn.coerceAtMost(snapshot.getLineLength(targetLine))

        val next = TextPosition(targetLine, targetColumn)

        return selection.withCaret(newCaret = next, keepAnchor = withSelection, stickyCol = selection.stickyColumn)
    }

    private fun moveSelectionDown(
        selection: Selection, snapshot: TextSnapshot, foldMap: FoldMap, withSelection: Boolean
    ): Selection {
        val currentVisual = foldMap.documentToVisualLine(selection.caret.line)

        if (currentVisual >= foldMap.totalVisualLines - 1) return selection

        val targetLine = foldMap.visualToDocumentLine(currentVisual + 1)

        val targetColumn = selection.stickyColumn.coerceAtMost(snapshot.getLineLength(targetLine))

        val next = TextPosition(targetLine, targetColumn)

        return selection.withCaret(newCaret = next, keepAnchor = withSelection, stickyCol = selection.stickyColumn)
    }

    private fun moveSelectionWordLeft(selection: Selection, snapshot: TextSnapshot, withSelection: Boolean): Selection {
        val current = selection.caret

        var next = current

        if (next.column == 0) {
            when {
                next.line > 0 -> {
                    next = TextPosition(next.line - 1, snapshot.getLineLength(next.line - 1))
                }

                else -> return selection
            }
        }

        val lineText = snapshot.getLineText(next.line).take(next.column)

        var column = next.column - 1

        while (column > 0 && lineText[column].isWhitespace()) column--

        when {
            column >= 0 && lineText[column].isLetterOrDigit() -> while (column > 0 && lineText[column - 1].isLetterOrDigit()) column--

            column > 0 -> column--
        }

        val target = TextPosition(next.line, column)

        return selection.withCaret(newCaret = target, keepAnchor = withSelection, stickyCol = target.column)
    }

    private fun moveSelectionWordRight(
        selection: Selection, snapshot: TextSnapshot, withSelection: Boolean
    ): Selection {
        val current = selection.caret

        val lineLen = snapshot.getLineLength(current.line)

        var next = current

        if (next.column >= lineLen) {
            when {
                next.line < snapshot.lines - 1 -> next = TextPosition(next.line + 1, 0)

                else -> return selection
            }
        }

        val lineText = snapshot.getLineText(next.line)

        var column = next.column

        val max = lineText.length

        if (max > 0) {
            when {
                lineText[column].isLetterOrDigit() -> while (column < max && lineText[column].isLetterOrDigit()) column++

                lineText[column].isWhitespace() -> {
                    while (column < max && lineText[column].isWhitespace()) column++

                    if (column < max && lineText[column].isLetterOrDigit()) {
                        while (column < max && lineText[column].isLetterOrDigit()) column++
                    }
                }

                else -> column++
            }
        }

        val target = TextPosition(next.line, column)

        return selection.withCaret(newCaret = target, keepAnchor = withSelection, stickyCol = target.column)
    }

    private fun moveSelectionLineStart(selection: Selection, state: EditorState, withSelection: Boolean): Selection {
        val lineText = state.snapshot.getLineText(selection.caret.line)

        val firstNonWhitespace = lineText.indexOfFirst { text ->
            !text.isWhitespace()
        }.let { index ->
            when (index) {
                -1 -> lineText.length

                else -> index
            }
        }

        val targetCol = when (selection.caret.column) {
            firstNonWhitespace -> 0

            else -> firstNonWhitespace
        }

        val target = TextPosition(selection.caret.line, targetCol)

        return selection.withCaret(newCaret = target, keepAnchor = withSelection, stickyCol = targetCol)
    }

    private fun moveSelectionLineEnd(selection: Selection, snapshot: TextSnapshot, withSelection: Boolean): Selection {
        val maxColumn = snapshot.getLineLength(selection.caret.line)

        val target = TextPosition(selection.caret.line, maxColumn)

        return selection.withCaret(newCaret = target, keepAnchor = withSelection, stickyCol = maxColumn)
    }

    private fun transformAll(state: EditorState, transform: (Selection) -> Selection): EditorResult {
        val newSelections = state.selections.map { selection ->
            transform(selection.coerceIn(state.snapshot))
        }

        return EditorResult(state.copy(selections = Selection.normalize(newSelections)))
    }

    fun reduce(state: EditorState, action: CaretAction) = when (action) {
        is CaretAction.Move -> {
            val valid = action.position.coerceIn(state.snapshot)

            val newSelections = when {
                action.withSelection -> state.selections.map { selection ->
                    selection.withCaret(valid, keepAnchor = true)
                }

                else -> listOf(Selection.cursor(valid))
            }

            EditorResult(state.copy(selections = Selection.normalize(newSelections)))
        }

        is CaretAction.AddCursor -> {
            val valid = action.position.coerceIn(state.snapshot)

            EditorResult(state.copy(selections = Selection.normalize(state.selections + Selection.cursor(valid))))
        }

        is CaretAction.MoveLeft -> transformAll(state) { selection ->
            moveSelectionLeft(selection, state.snapshot, state.foldMap, action.withSelection)
        }

        is CaretAction.MoveRight -> transformAll(state) { selection ->
            moveSelectionRight(selection, state.snapshot, state.foldMap, action.withSelection)
        }

        is CaretAction.MoveUp -> transformAll(state) { selection ->
            moveSelectionUp(selection, state.snapshot, state.foldMap, action.withSelection)
        }

        is CaretAction.MoveDown -> transformAll(state) { selection ->
            moveSelectionDown(selection, state.snapshot, state.foldMap, action.withSelection)
        }

        is CaretAction.MoveWordLeft -> transformAll(state) { selection ->
            moveSelectionWordLeft(selection, state.snapshot, action.withSelection)
        }

        is CaretAction.MoveWordRight -> transformAll(state) { selection ->
            moveSelectionWordRight(selection, state.snapshot, action.withSelection)
        }

        is CaretAction.MoveLineStart -> transformAll(state) { selection ->
            moveSelectionLineStart(selection, state, action.withSelection)
        }

        is CaretAction.MoveLineEnd -> transformAll(state) { selection ->
            moveSelectionLineEnd(selection, state.snapshot, action.withSelection)
        }

        is CaretAction.MoveDocStart -> {
            val target = TextPosition.ZERO

            val newSelections = when {
                action.withSelection -> state.selections.map { selection ->
                    selection.withCaret(target, keepAnchor = true)
                }

                else -> listOf(Selection.cursor(target))
            }

            EditorResult(state.copy(selections = Selection.normalize(newSelections)))
        }

        is CaretAction.MoveDocEnd -> {
            val target = state.snapshot.lastPosition

            val newSelections = when {
                action.withSelection -> state.selections.map { selection ->
                    selection.withCaret(target, keepAnchor = true)
                }

                else -> listOf(Selection.cursor(target))
            }

            EditorResult(state.copy(selections = Selection.normalize(newSelections)))
        }
    }
}