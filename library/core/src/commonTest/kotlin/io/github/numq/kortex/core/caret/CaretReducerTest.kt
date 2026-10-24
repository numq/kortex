package io.github.numq.kortex.core.caret

import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.snapshot.FakeTextSnapshot
import io.github.numq.krope.text.TextPosition
import kotlin.test.Test
import kotlin.test.assertEquals

class CaretReducerTest {
    @Test
    fun `moveLineStart toggles between first non-whitespace and column 0`() {
        val snapshot = FakeTextSnapshot("    val x = 1")
        val initial = EditorState(snapshot, selections = listOf(Selection.cursor(TextPosition(0, 10))))

        val firstPress = CaretReducer.reduce(initial, CaretAction.MoveLineStart(withSelection = false)).state
        assertEquals(TextPosition(0, 4), firstPress.caret.position)

        val secondPress = CaretReducer.reduce(firstPress, CaretAction.MoveLineStart(withSelection = false)).state
        assertEquals(TextPosition(0, 0), secondPress.caret.position)
    }

    @Test
    fun `moveDown and moveUp skip folded lines and preserve stickyColumn`() {
        val snapshot = FakeTextSnapshot("0123456789\nfolded1\nfolded2\nshort\n0123456789")
        val initial = EditorState(
            snapshot = snapshot,
            selections = listOf(Selection.cursor(TextPosition(0, 8), stickyColumn = 8)),
            collapsedRanges = listOf(0..2),
            collapsedLines = setOf(0)
        )

        val down1 = CaretReducer.reduce(initial, CaretAction.MoveDown()).state
        assertEquals(TextPosition(3, 5), down1.caret.position)
        assertEquals(8, down1.stickyColumn)

        val down2 = CaretReducer.reduce(down1, CaretAction.MoveDown()).state
        assertEquals(TextPosition(4, 8), down2.caret.position)
    }
}