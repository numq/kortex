package io.github.numq.kortex.core.selection

import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.snapshot.FakeTextSnapshot
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectionReducerTest {
    @Test
    fun `selectWordAt selects identifier or operator group`() {
        val snapshot = FakeTextSnapshot("foo_bar += 123")
        val state = EditorState(snapshot)

        val idRes = SelectionReducer.reduce(state, SelectionAction.SelectWordAt(TextPosition(0, 2))).state
        assertEquals(TextRange(TextPosition(0, 0), TextPosition(0, 7)), idRes.primarySelection.range)

        val opRes = SelectionReducer.reduce(state, SelectionAction.SelectWordAt(TextPosition(0, 8))).state
        assertEquals(TextRange(TextPosition(0, 8), TextPosition(0, 10)), opRes.primarySelection.range)
    }

    @Test
    fun `selectNextMatch selects current word then adds next occurrence`() {
        val snapshot = FakeTextSnapshot("foo bar foo baz")
        val state = EditorState(snapshot, selections = listOf(Selection.cursor(TextPosition(0, 1))))

        val first = SelectionReducer.reduce(state, SelectionAction.SelectNextMatch).state
        assertEquals(1, first.selections.size)
        assertEquals(TextRange(TextPosition(0, 0), TextPosition(0, 3)), first.primarySelection.range)

        val second = SelectionReducer.reduce(first, SelectionAction.SelectNextMatch).state
        assertEquals(2, second.selections.size)
        assertEquals(TextRange(TextPosition(0, 8), TextPosition(0, 11)), second.primarySelection.range)
    }
}