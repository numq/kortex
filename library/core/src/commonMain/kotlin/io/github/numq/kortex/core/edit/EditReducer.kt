package io.github.numq.kortex.core.edit

import io.github.numq.kortex.core.EditorCommand
import io.github.numq.kortex.core.EditorResult
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange
import kotlin.math.max

internal object EditReducer {
    private fun getPreviousCodePointStep(text: String, column: Int) = when {
        column <= 0 -> 0

        column >= 2 && text[column - 1].isLowSurrogate() && text[column - 2].isHighSurrogate() -> 2

        else -> 1
    }

    private fun getNextCodePointStep(text: String, column: Int) = when {
        column >= text.length -> 0

        column + 1 < text.length && text[column].isHighSurrogate() && text[column + 1].isLowSurrogate() -> 2

        else -> 1
    }

    private fun wrapOperations(operations: List<TextOperation.Data.Single>) = when {
        operations.isEmpty() -> null

        operations.size == 1 -> operations[0]

        else -> TextOperation.Data.Batch(operations)
    }

    private fun calculateInsert(state: EditorState, text: String): TextOperation.Data {
        val sortedSelections = state.selections.sortedDescending()

        val operations = sortedSelections.map { selection ->
            when {
                selection.isNotEmpty -> TextOperation.Data.Single.Replace(range = selection.range, text = text)

                else -> TextOperation.Data.Single.Insert(position = selection.caret, text = text)
            }
        }

        return wrapOperations(operations) ?: TextOperation.Data.Single.Insert(state.primarySelection.caret, text)
    }

    private fun calculateBackspace(state: EditorState, tabSize: Int): TextOperation.Data? {
        val sortedSelections = state.selections.sortedDescending()

        val operations = ArrayList<TextOperation.Data.Single>(sortedSelections.size)

        for (selection in sortedSelections) {
            when {
                selection.isNotEmpty -> operations.add(TextOperation.Data.Single.Delete(range = selection.range))

                else -> {
                    val position = selection.caret

                    if (position == TextPosition.ZERO) continue

                    val startPosition = when {
                        position.column > 0 -> {
                            val lineText = state.snapshot.getLineText(position.line)

                            val textBefore = lineText.take(position.column)

                            when {
                                textBefore.isNotEmpty() && textBefore.all { char ->
                                    char == ' '
                                } -> {
                                    val deleteCount = when (val remainder = position.column % tabSize) {
                                        0 -> tabSize

                                        else -> remainder
                                    }

                                    TextPosition(position.line, max(0, position.column - deleteCount))
                                }

                                else -> {
                                    val step = getPreviousCodePointStep(lineText, position.column)

                                    TextPosition(position.line, position.column - step)
                                }
                            }
                        }

                        position.line > 0 -> {
                            val prevLine = position.line - 1

                            TextPosition(line = prevLine, column = state.snapshot.getLineLength(prevLine))
                        }

                        else -> position
                    }

                    if (startPosition != position) {
                        operations.add(
                            TextOperation.Data.Single.Delete(
                                range = TextRange(
                                    start = startPosition, end = position
                                )
                            )
                        )
                    }
                }
            }
        }

        return wrapOperations(operations)
    }

    private fun calculateDelete(state: EditorState): TextOperation.Data? {
        val sortedSelections = state.selections.sortedDescending()

        val operations = ArrayList<TextOperation.Data.Single>(sortedSelections.size)

        for (selection in sortedSelections) {
            when {
                selection.isNotEmpty -> operations.add(TextOperation.Data.Single.Delete(range = selection.range))

                else -> {
                    val position = selection.caret

                    val lineLength = state.snapshot.getLineLength(position.line)

                    val endPosition = when {
                        position.column < lineLength -> {
                            val lineText = state.snapshot.getLineText(position.line)

                            val step = getNextCodePointStep(lineText, position.column)

                            TextPosition(position.line, position.column + step)
                        }

                        position.line < state.snapshot.lines - 1 -> TextPosition(line = position.line + 1, column = 0)

                        else -> position
                    }

                    if (position != endPosition) {
                        operations.add(
                            TextOperation.Data.Single.Delete(
                                range = TextRange(
                                    start = position, end = endPosition
                                )
                            )
                        )
                    }
                }
            }
        }

        return wrapOperations(operations)
    }

    private fun calculateEnter(state: EditorState): TextOperation.Data {
        val sortedSelections = state.selections.sortedDescending()

        val operations = sortedSelections.map { selection ->
            val currentLineText = state.snapshot.getLineText(selection.caret.line)

            val currentIndent = currentLineText.takeWhile { char ->
                char == ' ' || char == '\t'
            }

            val insertText = "\n$currentIndent"

            when {
                selection.isNotEmpty -> TextOperation.Data.Single.Replace(range = selection.range, text = insertText)

                else -> TextOperation.Data.Single.Insert(position = selection.caret, text = insertText)
            }
        }

        return wrapOperations(operations) ?: TextOperation.Data.Single.Insert(state.primarySelection.caret, "\n")
    }

    private fun calculateTab(state: EditorState, tabSpaces: String): TextOperation.Data {
        val hasRange = state.selections.any(Selection::isNotEmpty)

        if (!hasRange) {
            return calculateInsert(state, tabSpaces)
        }

        val uniqueLines = state.selections.flatMap { selection ->
            val start = selection.range.start.line

            val end = when {
                selection.range.end.column == 0 && selection.range.end.line > start -> selection.range.end.line - 1

                else -> selection.range.end.line
            }

            start..end
        }.distinct().sortedDescending()

        val operations = uniqueLines.map { line ->
            TextOperation.Data.Single.Insert(position = TextPosition(line, 0), text = tabSpaces)
        }

        return wrapOperations(operations) ?: TextOperation.Data.Single.Insert(state.primarySelection.caret, tabSpaces)
    }

    private fun calculateUntab(state: EditorState, tabSpaces: String, tabSize: Int): TextOperation.Data? {
        val uniqueLines = state.selections.flatMap { selection ->
            val start = when {
                selection.isNotEmpty -> selection.range.start.line

                else -> selection.caret.line
            }

            val end = when {
                selection.isNotEmpty -> when {
                    selection.range.end.column == 0 && selection.range.end.line > start -> selection.range.end.line - 1

                    else -> selection.range.end.line
                }

                else -> selection.caret.line
            }

            start..end
        }.distinct().sortedDescending()

        val operations = ArrayList<TextOperation.Data.Single>()

        for (line in uniqueLines) {
            val text = state.snapshot.getLineText(line)

            val spacesToRemove = when {
                text.startsWith(tabSpaces) -> tabSize

                text.startsWith("\t") -> 1

                else -> text.takeWhile { char ->
                    char == ' '
                }.length.coerceAtMost(tabSize)
            }

            if (spacesToRemove > 0) {
                operations.add(
                    TextOperation.Data.Single.Delete(
                        range = TextRange(TextPosition(line, 0), TextPosition(line, spacesToRemove))
                    )
                )
            }
        }

        return wrapOperations(operations)
    }

    private fun calculateMoveLineUp(state: EditorState): TextOperation.Data? {
        val minLine = state.selections.minOf { selection ->
            selection.range.start.line
        }

        val maxLine = state.selections.maxOf { selection ->
            when {
                selection.range.end.column == 0 && selection.range.end.line > selection.range.start.line -> selection.range.end.line - 1

                else -> selection.range.end.line
            }
        }

        if (minLine <= 0) return null

        val prevLineText = state.snapshot.getLineText(minLine - 1)

        val movingLines = (minLine..maxLine).map(state.snapshot::getLineText)

        val newBlockText = (movingLines + prevLineText).joinToString("\n")

        val replaceRange = TextRange(
            start = TextPosition(line = minLine - 1, column = 0),
            end = TextPosition(line = maxLine, column = state.snapshot.getLineLength(maxLine))
        )

        return TextOperation.Data.Single.Replace(range = replaceRange, text = newBlockText)
    }

    private fun calculateMoveLineDown(state: EditorState): TextOperation.Data? {
        val minLine = state.selections.minOf { selection ->
            selection.range.start.line
        }

        val maxLine = state.selections.maxOf { selection ->
            when {
                selection.range.end.column == 0 && selection.range.end.line > selection.range.start.line -> selection.range.end.line - 1

                else -> selection.range.end.line
            }
        }

        if (maxLine >= state.snapshot.lines - 1) return null

        val nextLineText = state.snapshot.getLineText(maxLine + 1)

        val movingLines = (minLine..maxLine).map(state.snapshot::getLineText)

        val newBlockText = (listOf(nextLineText) + movingLines).joinToString("\n")

        val replaceRange = TextRange(
            start = TextPosition(line = minLine, column = 0),
            end = TextPosition(line = maxLine + 1, column = state.snapshot.getLineLength(maxLine + 1))
        )

        return TextOperation.Data.Single.Replace(range = replaceRange, text = newBlockText)
    }

    private fun calculateJoinLines(state: EditorState): TextOperation.Data? {
        if (state.snapshot.lines <= 1) return null

        val lineIntervals = state.selections.mapNotNull { selection ->
            val start = selection.range.start.line

            val end = when {
                selection.isNotEmpty -> when {
                    selection.range.end.column == 0 && selection.range.end.line > start -> selection.range.end.line

                    else -> (selection.range.end.line + 1).coerceAtMost(state.snapshot.lines - 1)
                }

                else -> (start + 1).coerceAtMost(state.snapshot.lines - 1)
            }

            if (start < end) start..end else null
        }

        if (lineIntervals.isEmpty()) return null

        val sortedIntervals = lineIntervals.sortedBy(IntRange::first)

        val mergedIntervals = ArrayList<IntRange>()

        var curStart = sortedIntervals[0].first

        var curEnd = sortedIntervals[0].last

        for (i in 1 until sortedIntervals.size) {
            val next = sortedIntervals[i]

            when {
                next.first <= curEnd -> curEnd = maxOf(curEnd, next.last)

                else -> {
                    mergedIntervals.add(curStart..curEnd)

                    curStart = next.first

                    curEnd = next.last
                }
            }
        }

        mergedIntervals.add(curStart..curEnd)

        val operations = mergedIntervals.sortedByDescending { it.first }.map { interval ->
            val lines = (interval.first..interval.last).map(state.snapshot::getLineText)

            val joined = buildString {
                for (i in lines.indices) {
                    val line = lines[i]

                    when (i) {
                        0 -> append(line)

                        else -> {
                            val trimmed = line.trimStart()

                            if (isNotEmpty() && !endsWith(' ') && trimmed.isNotEmpty()) {
                                append(' ')
                            }

                            append(trimmed)
                        }
                    }
                }
            }

            val lastLineLen = state.snapshot.getLineLength(interval.last)

            TextOperation.Data.Single.Replace(
                range = TextRange(
                    start = TextPosition(interval.first, 0), end = TextPosition(interval.last, lastLineLen)
                ), text = joined
            )
        }

        return wrapOperations(operations)
    }

    private fun calculateToggleComment(state: EditorState, commentPrefix: String): TextOperation.Data {
        val uniqueLines = state.selections.flatMap { selection ->
            val start = when {
                selection.isNotEmpty -> selection.range.start.line

                else -> selection.caret.line
            }

            val end = when {
                selection.isNotEmpty -> when {
                    selection.range.end.column == 0 && selection.range.end.line > start -> selection.range.end.line - 1

                    else -> selection.range.end.line
                }

                else -> selection.caret.line
            }

            start..end
        }.distinct().sorted()

        val lines = uniqueLines.map(state.snapshot::getLineText)

        val trimmedPrefix = commentPrefix.trimEnd()

        val nonEmptyLines = lines.filter(String::isNotBlank)

        val allCommented = nonEmptyLines.isNotEmpty() && nonEmptyLines.all { line ->
            val trimmed = line.trimStart()

            trimmed.startsWith(commentPrefix) || trimmed.startsWith(trimmedPrefix)
        }

        val operations = ArrayList<TextOperation.Data.Single>()

        when {
            allCommented -> for (line in uniqueLines.asReversed()) {
                val lineText = state.snapshot.getLineText(line)

                val indent = lineText.takeWhile { char ->
                    char == ' ' || char == '\t'
                }

                val content = lineText.drop(indent.length)

                val removeLen = when {
                    content.startsWith(commentPrefix) -> commentPrefix.length

                    content.startsWith(trimmedPrefix) -> trimmedPrefix.length

                    else -> 0
                }

                if (removeLen > 0) {
                    operations.add(
                        TextOperation.Data.Single.Delete(
                            range = TextRange(
                                TextPosition(line, indent.length), TextPosition(line, indent.length + removeLen)
                            )
                        )
                    )
                }
            }

            else -> {
                val minIndent = nonEmptyLines.minOfOrNull { text ->
                    text.takeWhile { char ->
                        char == ' ' || char == '\t'
                    }.length
                } ?: 0

                for (line in uniqueLines.asReversed()) {
                    val lineText = state.snapshot.getLineText(line)

                    if (lineText.isNotBlank()) {
                        operations.add(
                            TextOperation.Data.Single.Insert(
                                position = TextPosition(line, minIndent), text = commentPrefix
                            )
                        )
                    }
                }
            }
        }

        return wrapOperations(operations) ?: TextOperation.Data.Single.Insert(
            state.primarySelection.caret, commentPrefix
        )
    }

    private fun calculateDuplicate(state: EditorState): TextOperation.Data {
        val sorted = state.selections.sortedDescending()

        val operations = sorted.map { selection ->
            when {
                selection.isNotEmpty -> {
                    val text = state.snapshot.getTextInRange(selection.range)

                    TextOperation.Data.Single.Insert(position = selection.range.end, text = text)
                }

                else -> {
                    val line = selection.caret.line

                    val lineLength = state.snapshot.getLineLength(line)

                    val lineText =
                        state.snapshot.getTextInRange(TextRange(TextPosition(line, 0), TextPosition(line, lineLength)))

                    when {
                        line < state.snapshot.lines - 1 -> TextOperation.Data.Single.Insert(
                            position = TextPosition(
                                line + 1, 0
                            ), text = "$lineText\n"
                        )

                        else -> TextOperation.Data.Single.Insert(
                            position = TextPosition(line, lineLength), text = "\n$lineText"
                        )
                    }
                }
            }
        }

        return wrapOperations(operations) ?: TextOperation.Data.Single.Insert(state.primarySelection.caret, "")
    }

    private fun calculateWordDeleteLeft(state: EditorState): TextOperation.Data? {
        val sorted = state.selections.sortedDescending()

        val operations = ArrayList<TextOperation.Data.Single>()

        for (selection in sorted) {
            when {
                selection.isNotEmpty -> operations.add(TextOperation.Data.Single.Delete(selection.range))

                else -> {
                    val current = selection.caret

                    if (current == TextPosition.ZERO) continue

                    if (current.column == 0 && current.line > 0) {
                        val prevLineEnd = TextPosition(current.line - 1, state.snapshot.getLineLength(current.line - 1))

                        operations.add(TextOperation.Data.Single.Delete(TextRange(prevLineEnd, current)))

                        continue
                    }

                    val lineText = state.snapshot.getLineText(current.line).take(current.column)

                    var column = current.column - getPreviousCodePointStep(lineText, current.column)

                    while (column > 0 && lineText[column].isWhitespace()) {
                        column -= getPreviousCodePointStep(lineText, column)
                    }

                    if (column >= 0 && lineText[column].isLetterOrDigit()) {
                        while (column > 0 && lineText[column - 1].isLetterOrDigit()) {
                            column -= getPreviousCodePointStep(lineText, column)
                        }
                    }

                    val target = TextPosition(current.line, max(0, column))

                    operations.add(TextOperation.Data.Single.Delete(TextRange(target, current)))
                }
            }
        }

        return wrapOperations(operations)
    }

    private fun calculateWordDeleteRight(state: EditorState): TextOperation.Data? {
        val sorted = state.selections.sortedDescending()

        val operations = ArrayList<TextOperation.Data.Single>()

        for (selection in sorted) {
            when {
                selection.isNotEmpty -> operations.add(TextOperation.Data.Single.Delete(selection.range))

                else -> {
                    val current = selection.caret

                    val lineLength = state.snapshot.getLineLength(current.line)

                    if (current.column >= lineLength) {
                        if (current.line < state.snapshot.lines - 1) {
                            operations.add(
                                TextOperation.Data.Single.Delete(
                                    TextRange(
                                        current, TextPosition(current.line + 1, 0)
                                    )
                                )
                            )
                        }

                        continue
                    }

                    val lineText = state.snapshot.getLineText(current.line)

                    var column = current.column

                    val max = lineText.length

                    if (max > 0) {
                        when {
                            lineText[column].isLetterOrDigit() -> while (column < max && lineText[column].isLetterOrDigit()) {
                                column += getNextCodePointStep(lineText, column)
                            }

                            lineText[column].isWhitespace() -> {
                                while (column < max && lineText[column].isWhitespace()) {
                                    column += getNextCodePointStep(lineText, column)
                                }

                                if (column < max && lineText[column].isLetterOrDigit()) {
                                    while (column < max && lineText[column].isLetterOrDigit()) {
                                        column += getNextCodePointStep(lineText, column)
                                    }
                                }
                            }

                            else -> column += getNextCodePointStep(lineText, column)
                        }
                    }

                    val target = TextPosition(current.line, column)

                    operations.add(TextOperation.Data.Single.Delete(TextRange(current, target)))
                }
            }
        }

        return wrapOperations(operations)
    }

    fun reduce(state: EditorState, action: EditAction) = when (action) {
        is EditAction.Insert -> {
            val operation = calculateInsert(state, action.text)

            EditorResult(state, EditorCommand.ApplyText(operation, canMerge = true))
        }

        is EditAction.Paste -> {
            val operation = calculateInsert(state, action.text)

            EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }

        is EditAction.Backspace -> {
            val hasSelection = state.selections.any(Selection::isNotEmpty)

            when (val operation = calculateBackspace(state, state.config.tabSize)) {
                null -> EditorResult(state)

                else -> EditorResult(state, EditorCommand.ApplyText(operation, canMerge = !hasSelection))
            }
        }

        is EditAction.Delete -> {
            val hasSelection = state.selections.any(Selection::isNotEmpty)

            when (val operation = calculateDelete(state)) {
                null -> EditorResult(state)

                else -> EditorResult(state, EditorCommand.ApplyText(operation, canMerge = !hasSelection))
            }
        }

        is EditAction.Enter -> {
            val operation = calculateEnter(state)

            EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }

        is EditAction.Tab -> {
            val operation = calculateTab(state, state.config.tabString)

            EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }

        is EditAction.Untab -> when (val operation =
            calculateUntab(state, state.config.tabString, state.config.tabSize)) {
            null -> EditorResult(state)

            else -> EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }

        is EditAction.MoveLineUp -> when (val operation = calculateMoveLineUp(state)) {
            null -> EditorResult(state)

            else -> EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }

        is EditAction.MoveLineDown -> when (val operation = calculateMoveLineDown(state)) {
            null -> EditorResult(state)

            else -> EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }

        is EditAction.JoinLines -> when (val operation = calculateJoinLines(state)) {
            null -> EditorResult(state)

            else -> EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }

        is EditAction.Duplicate -> {
            val operation = calculateDuplicate(state)

            EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }

        is EditAction.WordDeleteLeft -> when (val operation = calculateWordDeleteLeft(state)) {
            null -> EditorResult(state)

            else -> EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }

        is EditAction.WordDeleteRight -> when (val operation = calculateWordDeleteRight(state)) {
            null -> EditorResult(state)

            else -> EditorResult(state, EditorCommand.ApplyText(operation, canMerge = false))
        }
    }
}