package io.github.numq.kortex.core.selection

import io.github.numq.kortex.core.EditorResult
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.text.offsetOf
import io.github.numq.kortex.core.text.positionOf
import io.github.numq.krope.text.TextPosition

internal object SelectionReducer {
    private fun clear(state: EditorState) = when {
        state.selections.size > 1 -> state.copy(selections = listOf(Selection.cursor(state.primarySelection.caret)))

        else -> {
            val primary = state.primarySelection

            when {
                primary.isNotEmpty -> state.copy(selections = listOf(Selection.cursor(primary.caret)))

                else -> state
            }
        }
    }

    private fun selectAll(state: EditorState): EditorState {
        val start = TextPosition.ZERO

        val end = state.snapshot.lastPosition

        return state.copy(selections = listOf(Selection.fromPositions(anchor = start, caret = end)))
    }

    private fun selectWordAt(state: EditorState, position: TextPosition): EditorState {
        val lineText = state.snapshot.getLineText(position.line)

        if (lineText.isEmpty()) return state

        val column = position.column.coerceIn(0, (lineText.length - 1).coerceAtLeast(0))

        val firstChar = lineText.getOrNull(column) ?: return state

        val isIdentifier = firstChar.isLetterOrDigit() || firstChar == '_' || firstChar == '\''

        val isOperator = ":!#$%&*+./<=>?@\\^|-~".contains(firstChar)

        if (!isIdentifier && !isOperator) {
            val start = TextPosition(line = position.line, column = column)

            val end = TextPosition(line = position.line, column = column + 1)

            return state.copy(selections = listOf(Selection.fromPositions(anchor = start, caret = end)))
        }

        val predicate: (Char) -> Boolean = if (isIdentifier) { char ->
            char.isLetterOrDigit() || char == '_' || char == '\''
        } else { char ->
            ":!#$%&*+./<=>?@\\^|-~".contains(char)
        }

        var startColumn = column

        while (startColumn > 0 && predicate(lineText[startColumn - 1])) startColumn--

        var endColumn = column

        while (endColumn < lineText.length && predicate(lineText[endColumn])) endColumn++

        val wordSelection = Selection.fromPositions(
            anchor = TextPosition(line = position.line, column = startColumn),
            caret = TextPosition(line = position.line, column = endColumn)
        )

        return state.copy(selections = listOf(wordSelection))
    }

    private fun selectNextMatch(state: EditorState): EditorState {
        val primary = state.primarySelection

        if (primary.isEmpty) {
            return selectWordAt(state, primary.caret)
        }

        val query = state.snapshot.getTextInRange(primary.range)

        if (query.isEmpty()) return state

        val fullText = state.snapshot.text

        val queryLength = query.length

        val primaryEndOffset = state.snapshot.offsetOf(primary.range.end)

        var nextIndex = fullText.indexOf(query, primaryEndOffset)

        if (nextIndex == -1) {
            nextIndex = fullText.indexOf(query, 0)
        }

        if (nextIndex != -1) {
            val nextStart = state.snapshot.positionOf(nextIndex)

            val nextEnd = state.snapshot.positionOf(nextIndex + queryLength)

            val nextSelection = Selection.fromPositions(anchor = nextStart, caret = nextEnd)

            return state.copy(selections = Selection.normalize(state.selections + nextSelection))
        }

        return state
    }

    fun reduce(state: EditorState, action: SelectionAction) = when (action) {
        is SelectionAction.Clear -> EditorResult(clear(state))

        is SelectionAction.SelectAll -> EditorResult(selectAll(state))

        is SelectionAction.SelectWordAt -> EditorResult(selectWordAt(state, action.position))

        is SelectionAction.SelectLine -> {
            val total = state.snapshot.lines

            when {
                action.line in 0 until total -> {
                    val selection = Selection.fromPositions(
                        anchor = TextPosition(action.line, 0),
                        caret = TextPosition(action.line, state.snapshot.getLineLength(action.line))
                    )

                    EditorResult(state.copy(selections = listOf(selection)))
                }

                else -> EditorResult(state)
            }
        }

        is SelectionAction.SelectRange -> EditorResult(state.copy(selections = listOf(Selection.fromRange(action.range))))

        is SelectionAction.AddSelection -> EditorResult(state.copy(selections = Selection.normalize(state.selections + action.selection)))

        is SelectionAction.SetSelections -> EditorResult(state.copy(selections = Selection.normalize(action.selections)))

        is SelectionAction.SelectNextMatch -> EditorResult(selectNextMatch(state))
    }
}