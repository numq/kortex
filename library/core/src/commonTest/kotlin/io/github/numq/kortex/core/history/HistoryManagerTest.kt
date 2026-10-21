package io.github.numq.kortex.core.history

import io.github.numq.kortex.core.caret.Caret
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import kotlin.test.*
import kotlin.time.TimeSource

class HistoryManagerTest {
    @Test
    fun `merges contiguous single-character inserts within time window`() {
        val history = HistoryManager(maxHistorySize = 50)
        val timeSource = TimeSource.Monotonic

        val now = timeSource.markNow()

        val entry1 = HistoryEntry(
            forward = TextOperation.Data.Single.Insert(TextPosition(0, 0), "a"),
            backward = TextOperation.Data.Single.Delete(
                io.github.numq.krope.text.TextRange(
                    TextPosition(0, 0), TextPosition(0, 1)
                )
            ),
            caretBefore = Caret(TextPosition(0, 0)),
            selectionBefore = Selection.cursor(TextPosition(0, 0)),
            caretAfter = Caret(TextPosition(0, 1)),
            selectionAfter = Selection.cursor(TextPosition(0, 1)),
            timestamp = now,
            canMerge = true
        )

        val entry2 = HistoryEntry(
            forward = TextOperation.Data.Single.Insert(TextPosition(0, 1), "b"),
            backward = TextOperation.Data.Single.Delete(
                io.github.numq.krope.text.TextRange(
                    TextPosition(0, 1), TextPosition(0, 2)
                )
            ),
            caretBefore = Caret(TextPosition(0, 1)),
            selectionBefore = Selection.cursor(TextPosition(0, 1)),
            caretAfter = Caret(TextPosition(0, 2)),
            selectionAfter = Selection.cursor(TextPosition(0, 2)),
            timestamp = now,
            canMerge = true
        )

        history.record(entry1)
        history.record(entry2)

        assertTrue(history.canUndo)

        val undone = history.popUndo()
        assertNotNull(undone)

        val op = undone.forward as TextOperation.Data.Single.Insert
        assertEquals("ab", op.text)
        assertEquals(TextPosition(0, 0), op.position)

        assertFalse(history.canUndo)
        assertTrue(history.canRedo)
    }

    @Test
    fun `does not merge inserts containing newline`() {
        val history = HistoryManager()
        val now = TimeSource.Monotonic.markNow()

        val entry1 = HistoryEntry(
            forward = TextOperation.Data.Single.Insert(TextPosition(0, 0), "a"),
            backward = TextOperation.Data.Single.Delete(
                io.github.numq.krope.text.TextRange(
                    TextPosition(0, 0), TextPosition(0, 1)
                )
            ),
            caretBefore = Caret(TextPosition(0, 0)),
            selectionBefore = Selection.cursor(TextPosition(0, 0)),
            caretAfter = Caret(TextPosition(0, 1)),
            selectionAfter = Selection.cursor(TextPosition(0, 1)),
            timestamp = now,
            canMerge = true
        )

        val entry2 = HistoryEntry(
            forward = TextOperation.Data.Single.Insert(TextPosition(0, 1), "\n"),
            backward = TextOperation.Data.Single.Delete(
                io.github.numq.krope.text.TextRange(
                    TextPosition(0, 1), TextPosition(1, 0)
                )
            ),
            caretBefore = Caret(TextPosition(0, 1)),
            selectionBefore = Selection.cursor(TextPosition(0, 1)),
            caretAfter = Caret(TextPosition(1, 0)),
            selectionAfter = Selection.cursor(TextPosition(1, 0)),
            timestamp = now,
            canMerge = true
        )

        history.record(entry1)
        history.record(entry2)

        val undone1 = history.popUndo()
        val undone2 = history.popUndo()

        assertNotNull(undone1)
        assertNotNull(undone2)
        assertEquals("\n", (undone1.forward as TextOperation.Data.Single.Insert).text)
        assertEquals("a", (undone2.forward as TextOperation.Data.Single.Insert).text)
    }
}