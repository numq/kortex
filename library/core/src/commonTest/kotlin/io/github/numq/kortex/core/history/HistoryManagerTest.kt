package io.github.numq.kortex.core.history

import io.github.numq.kortex.core.caret.Caret
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class HistoryManagerTest {
    @Test
    fun `merges contiguous single-character inserts within time window`() {
        val history = HistoryManager(maxHistorySize = 50)
        val now = TimeSource.Monotonic.markNow()

        val entry1 = HistoryEntry(
            forward = TextOperation.Data.Single.Insert(TextPosition(0, 0), "a"),
            backward = TextOperation.Data.Single.Delete(TextRange(TextPosition(0, 0), TextPosition(0, 1))),
            caretBefore = Caret(TextPosition(0, 0)),
            selectionBefore = Selection.cursor(TextPosition(0, 0)),
            caretAfter = Caret(TextPosition(0, 1)),
            selectionAfter = Selection.cursor(TextPosition(0, 1)),
            timestamp = now,
            canMerge = true
        )

        val entry2 = HistoryEntry(
            forward = TextOperation.Data.Single.Insert(TextPosition(0, 1), "b"),
            backward = TextOperation.Data.Single.Delete(TextRange(TextPosition(0, 1), TextPosition(0, 2))),
            caretBefore = Caret(TextPosition(0, 1)),
            selectionBefore = Selection.cursor(TextPosition(0, 1)),
            caretAfter = Caret(TextPosition(0, 2)),
            selectionAfter = Selection.cursor(TextPosition(0, 2)),
            timestamp = now + 100.milliseconds,
            canMerge = true
        )

        history.record(entry1)
        history.record(entry2)

        val undone = history.popUndo()
        assertNotNull(undone)
        val op = undone.forward as TextOperation.Data.Single.Insert
        assertEquals("ab", op.text)
        assertEquals(TextPosition(0, 0), op.position)
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)
    }

    @Test
    fun `merges contiguous single-line backspace deletes within time window`() {
        val history = HistoryManager()
        val now = TimeSource.Monotonic.markNow()

        val del1 = HistoryEntry(
            forward = TextOperation.Data.Single.Delete(TextRange(TextPosition(0, 1), TextPosition(0, 2))),
            backward = TextOperation.Data.Single.Insert(TextPosition(0, 1), "b"),
            selectionsBefore = listOf(Selection.cursor(TextPosition(0, 2))),
            selectionsAfter = listOf(Selection.cursor(TextPosition(0, 1))),
            timestamp = now,
            canMerge = true
        )

        val del2 = HistoryEntry(
            forward = TextOperation.Data.Single.Delete(TextRange(TextPosition(0, 0), TextPosition(0, 1))),
            backward = TextOperation.Data.Single.Insert(TextPosition(0, 0), "a"),
            selectionsBefore = listOf(Selection.cursor(TextPosition(0, 1))),
            selectionsAfter = listOf(Selection.cursor(TextPosition(0, 0))),
            timestamp = now + 100.milliseconds,
            canMerge = true
        )

        history.record(del1)
        history.record(del2)

        val merged = history.popUndo()
        assertNotNull(merged)
        val forwardDelete = merged.forward as TextOperation.Data.Single.Delete
        val backwardInsert = merged.backward as TextOperation.Data.Single.Insert

        assertEquals(TextRange(TextPosition(0, 0), TextPosition(0, 2)), forwardDelete.range)
        assertEquals("ab", backwardInsert.text)
        assertEquals(TextPosition(0, 0), backwardInsert.position)
    }

    @Test
    fun `does not merge entries outside 700ms window`() {
        val history = HistoryManager()
        val now = TimeSource.Monotonic.markNow()

        val entry1 = HistoryEntry(
            forward = TextOperation.Data.Single.Insert(TextPosition(0, 0), "a"),
            backward = TextOperation.Data.Single.Delete(TextRange(TextPosition(0, 0), TextPosition(0, 1))),
            selectionsBefore = listOf(Selection.ZERO),
            selectionsAfter = listOf(Selection.cursor(TextPosition(0, 1))),
            timestamp = now,
            canMerge = true
        )

        val entry2 = entry1.copy(
            forward = TextOperation.Data.Single.Insert(TextPosition(0, 1), "b"),
            selectionsBefore = listOf(Selection.cursor(TextPosition(0, 1))),
            selectionsAfter = listOf(Selection.cursor(TextPosition(0, 2))),
            timestamp = now + 800.milliseconds
        )

        history.record(entry1)
        history.record(entry2)

        assertNotNull(history.popUndo())
        assertNotNull(history.popUndo())
        assertFalse(history.canUndo)
    }

    @Test
    fun `popRedo moves entry back to undoStack`() {
        val history = HistoryManager()
        val now = TimeSource.Monotonic.markNow()

        val entry = HistoryEntry(
            forward = TextOperation.Data.Single.Insert(TextPosition(0, 0), "x"),
            backward = TextOperation.Data.Single.Delete(TextRange(TextPosition(0, 0), TextPosition(0, 1))),
            selectionsBefore = listOf(Selection.ZERO),
            selectionsAfter = listOf(Selection.cursor(TextPosition(0, 1))),
            timestamp = now,
            canMerge = false
        )

        history.record(entry)
        assertNotNull(history.popUndo())
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)

        val redone = history.popRedo()
        assertNotNull(redone)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `evicts oldest entry when maxHistorySize is reached`() {
        val history = HistoryManager(maxHistorySize = 2)
        val now = TimeSource.Monotonic.markNow()

        repeat(3) { idx ->
            history.record(
                HistoryEntry(
                    forward = TextOperation.Data.Single.Insert(TextPosition(0, idx), "$idx"),
                    backward = TextOperation.Data.Single.Delete(
                        TextRange(
                            TextPosition(0, idx), TextPosition(0, idx + 1)
                        )
                    ),
                    selectionsBefore = listOf(Selection.cursor(TextPosition(0, idx))),
                    selectionsAfter = listOf(Selection.cursor(TextPosition(0, idx + 1))),
                    timestamp = now,
                    canMerge = false
                )
            )
        }

        val last = history.popUndo()?.forward as TextOperation.Data.Single.Insert
        val prev = history.popUndo()?.forward as TextOperation.Data.Single.Insert
        assertNull(history.popUndo())

        assertEquals("2", last.text)
        assertEquals("1", prev.text)
    }
}