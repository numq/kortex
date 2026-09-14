package io.github.numq.kortex.code.comment

import io.github.numq.kortex.core.EditorState
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange

internal object CommentOperations {
    fun calculateToggleComment(state: EditorState, commentPrefix: String): TextOperation.Data {
        val uniqueLines = state.selections.flatMap { selection ->
            val start = when {
                selection.isNotEmpty -> selection.range.start.line

                else -> selection.caret.line
            }

            val end = when {
                selection.isNotEmpty -> {
                    when {
                        selection.range.end.column == 0 && selection.range.end.line > start -> selection.range.end.line - 1

                        else -> selection.range.end.line
                    }
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

        val ops = ArrayList<TextOperation.Data.Single>()

        when {
            allCommented -> for (line in uniqueLines.asReversed()) {
                val lineText = state.snapshot.getLineText(line)

                val indent = lineText.takeWhile { text ->
                    text == ' ' || text == '\t'
                }

                val content = lineText.drop(indent.length)

                val removeLen = when {
                    content.startsWith(commentPrefix) -> commentPrefix.length

                    content.startsWith(trimmedPrefix) -> trimmedPrefix.length

                    else -> 0
                }
                if (removeLen > 0) {
                    ops.add(
                        TextOperation.Data.Single.Delete(
                            range = TextRange(
                                TextPosition(line, indent.length), TextPosition(line, indent.length + removeLen)
                            )
                        )
                    )
                }
            }

            else -> {
                val minIndent = nonEmptyLines.minOfOrNull { line ->
                    line.takeWhile { c -> c == ' ' || c == '\t' }.length
                } ?: 0

                for (line in uniqueLines.asReversed()) {
                    val lineText = state.snapshot.getLineText(line)

                    if (lineText.isNotBlank()) {
                        ops.add(
                            TextOperation.Data.Single.Insert(
                                position = TextPosition(line, minIndent), text = commentPrefix
                            )
                        )
                    }
                }
            }
        }

        return when {
            ops.isEmpty() -> TextOperation.Data.Single.Insert(state.primarySelection.caret, commentPrefix)

            ops.size == 1 -> ops[0]

            else -> TextOperation.Data.Batch(ops)
        }
    }
}