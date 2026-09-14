package io.github.numq.kortex.core.selection

import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange
import io.github.numq.krope.text.TextSnapshot

data class Selection(
    val anchor: TextPosition, val caret: TextPosition, val stickyColumn: Int = caret.column
) : Comparable<Selection> {
    val isForward: Boolean get() = anchor <= caret

    val direction: SelectionDirection
        get() = when {
            isForward -> SelectionDirection.FORWARD

            else -> SelectionDirection.BACKWARD
        }

    val range: TextRange
        get() = when {
            isForward -> TextRange(anchor, caret)

            else -> TextRange(caret, anchor)
        }

    val isEmpty: Boolean get() = anchor == caret

    val isNotEmpty: Boolean get() = anchor != caret

    fun coerceIn(snapshot: TextSnapshot): Selection {
        val coercedAnchor = anchor.coerceIn(snapshot)

        val coercedCaret = caret.coerceIn(snapshot)

        return when {
            coercedAnchor == anchor && coercedCaret == caret -> this

            else -> copy(anchor = coercedAnchor, caret = coercedCaret, stickyColumn = coercedCaret.column)
        }
    }

    fun withCaret(newCaret: TextPosition, keepAnchor: Boolean, stickyCol: Int = newCaret.column): Selection {
        val newAnchor = when {
            keepAnchor -> anchor

            else -> newCaret
        }

        return Selection(anchor = newAnchor, caret = newCaret, stickyColumn = stickyCol)
    }

    override fun compareTo(other: Selection) = compareValuesBy(this, other, { it.range.start }, { it.range.end })

    companion object {
        val ZERO = Selection(anchor = TextPosition.ZERO, caret = TextPosition.ZERO, stickyColumn = 0)

        val EMPTY = ZERO

        fun cursor(position: TextPosition, stickyColumn: Int = position.column) =
            Selection(anchor = position, caret = position, stickyColumn = stickyColumn)

        fun fromPositions(anchor: TextPosition, caret: TextPosition, stickyColumn: Int = caret.column) =
            Selection(anchor = anchor, caret = caret, stickyColumn = stickyColumn)

        fun fromRange(range: TextRange, direction: SelectionDirection = SelectionDirection.FORWARD): Selection {
            val anchor = when (direction) {
                SelectionDirection.FORWARD -> range.start

                else -> range.end
            }

            val caret = when (direction) {
                SelectionDirection.FORWARD -> range.end

                else -> range.start
            }

            return Selection(anchor = anchor, caret = caret, stickyColumn = caret.column)
        }

        fun normalize(selections: List<Selection>): List<Selection> {
            if (selections.size <= 1) return selections.ifEmpty { listOf(ZERO) }

            val sorted = selections.sorted()

            val result = ArrayList<Selection>(sorted.size)

            var current = sorted[0]

            for (i in 1 until sorted.size) {
                val next = sorted[i]

                val shouldMerge = when {
                    current.isEmpty && next.isEmpty -> current.caret == next.caret

                    else -> current.range.intersects(next.range) || current.range.end == next.range.start
                }

                when {
                    shouldMerge -> {
                        val newStart = minOf(current.range.start, next.range.start)

                        val newEnd = maxOf(current.range.end, next.range.end)

                        val isForward = next.direction == SelectionDirection.FORWARD

                        val anchor = when {
                            isForward -> newStart

                            else -> newEnd
                        }

                        val caret = when {
                            isForward -> newEnd

                            else -> newStart
                        }

                        current = Selection(anchor = anchor, caret = caret, stickyColumn = caret.column)
                    }

                    else -> {
                        result.add(current)

                        current = next
                    }
                }
            }

            result.add(current)

            return result
        }
    }
}