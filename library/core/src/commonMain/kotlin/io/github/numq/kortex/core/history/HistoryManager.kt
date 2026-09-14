package io.github.numq.kortex.core.history

import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange
import kotlin.time.Duration.Companion.milliseconds

class HistoryManager(private val maxHistorySize: Int = 200) {
    private val undoStack = ArrayDeque<HistoryEntry>()

    private val redoStack = ArrayDeque<HistoryEntry>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()

    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(entry: HistoryEntry) {
        redoStack.clear()

        val last = undoStack.lastOrNull()

        when {
            last != null && shouldMerge(last, entry) -> {
                val merged = mergeEntries(last, entry)

                undoStack.removeLast()

                undoStack.addLast(merged)
            }

            else -> {
                if (undoStack.size >= maxHistorySize) {
                    undoStack.removeFirst()
                }

                undoStack.addLast(entry)
            }
        }
    }

    fun popUndo() = undoStack.removeLastOrNull()?.also(redoStack::addLast)

    fun popRedo() = redoStack.removeLastOrNull()?.also(redoStack::addLast)

    fun clear() {
        undoStack.clear()

        redoStack.clear()
    }

    private fun shouldMerge(prev: HistoryEntry, next: HistoryEntry): Boolean {
        if (!prev.canMerge || !next.canMerge) return false

        if ((next.timestamp - prev.timestamp) > 700.milliseconds) return false

        val prevOp = prev.forward

        val nextOp = next.forward

        val canMergeInserts =
            prevOp is TextOperation.Data.Single.Insert && nextOp is TextOperation.Data.Single.Insert && !prevOp.text.contains(
                '\n'
            ) && !nextOp.text.contains('\n') && prev.caretAfter.position == next.caretBefore.position

        val canMergeDeletes =
            prevOp is TextOperation.Data.Single.Delete && nextOp is TextOperation.Data.Single.Delete && prevOp.range.isSingleLine && nextOp.range.isSingleLine && prevOp.range.start == nextOp.range.end && prev.backward is TextOperation.Data.Single.Insert && next.backward is TextOperation.Data.Single.Insert

        return canMergeInserts || canMergeDeletes
    }

    private fun mergeEntries(prev: HistoryEntry, next: HistoryEntry): HistoryEntry {
        val prevOp = prev.forward

        val nextOp = next.forward

        return when (prevOp) {
            is TextOperation.Data.Single.Insert if nextOp is TextOperation.Data.Single.Insert -> {
                val mergedText = prevOp.text + nextOp.text

                val mergedForward = TextOperation.Data.Single.Insert(prevOp.position, mergedText)

                val endPos = TextPosition(prevOp.position.line, prevOp.position.column + mergedText.length)

                val mergedBackward = TextOperation.Data.Single.Delete(TextRange(prevOp.position, endPos))

                HistoryEntry(
                    forward = mergedForward,
                    backward = mergedBackward,
                    selectionsBefore = prev.selectionsBefore,
                    selectionsAfter = next.selectionsAfter,
                    timestamp = next.timestamp,
                    canMerge = true
                )
            }

            is TextOperation.Data.Single.Delete if nextOp is TextOperation.Data.Single.Delete -> {
                val prevBackInsert = prev.backward as TextOperation.Data.Single.Insert

                val nextBackInsert = next.backward as TextOperation.Data.Single.Insert

                val restoredText = nextBackInsert.text + prevBackInsert.text

                val mergedForward = TextOperation.Data.Single.Delete(
                    TextRange(nextOp.range.start, prevOp.range.end)
                )

                val mergedBackward = TextOperation.Data.Single.Insert(
                    position = nextOp.range.start, text = restoredText
                )

                HistoryEntry(
                    forward = mergedForward,
                    backward = mergedBackward,
                    selectionsBefore = prev.selectionsBefore,
                    selectionsAfter = next.selectionsAfter,
                    timestamp = next.timestamp,
                    canMerge = true
                )
            }

            else -> next
        }
    }
}