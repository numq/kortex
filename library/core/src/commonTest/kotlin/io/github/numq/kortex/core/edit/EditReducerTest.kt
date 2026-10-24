package io.github.numq.kortex.core.edit

import io.github.numq.kortex.core.EditorCommand
import io.github.numq.kortex.core.EditorConfig
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.snapshot.FakeTextSnapshot
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditReducerTest {
    @Test
    fun `backspace removes aligned soft tab spaces`() {
        val state = EditorState(
            snapshot = FakeTextSnapshot("    val x = 1"),
            selections = listOf(Selection.cursor(TextPosition(0, 4))),
            config = EditorConfig(tabSize = 4, useSoftTabs = true)
        )

        val result = EditReducer.reduce(state, EditAction.Backspace)
        val cmd = result.command as EditorCommand.ApplyText
        val deleteOp = cmd.operation as TextOperation.Data.Single.Delete

        assertEquals(TextRange(TextPosition(0, 0), TextPosition(0, 4)), deleteOp.range)
        assertTrue(cmd.canMerge)
    }

    @Test
    fun `backspace handles surrogate pairs`() {
        val emoji = "\uD83D\uDE00" // 😀 (2 UTF-16 code units)
        val state = EditorState(
            snapshot = FakeTextSnapshot("a${emoji}b"), selections = listOf(Selection.cursor(TextPosition(0, 3)))
        )

        val result = EditReducer.reduce(state, EditAction.Backspace)
        val cmd = result.command as EditorCommand.ApplyText
        val deleteOp = cmd.operation as TextOperation.Data.Single.Delete

        assertEquals(TextRange(TextPosition(0, 1), TextPosition(0, 3)), deleteOp.range)
    }

    @Test
    fun `enter preserves current line indentation`() {
        val state = EditorState(
            snapshot = FakeTextSnapshot("    fun test() {"), selections = listOf(Selection.cursor(TextPosition(0, 16)))
        )

        val result = EditReducer.reduce(state, EditAction.Enter)
        val cmd = result.command as EditorCommand.ApplyText
        val insertOp = cmd.operation as TextOperation.Data.Single.Insert

        assertEquals("\n    ", insertOp.text)
        assertFalse(cmd.canMerge)
    }

    @Test
    fun `tab and untab indent and unindent multiline selection`() {
        val state = EditorState(
            snapshot = FakeTextSnapshot("  line1\n    line2"),
            selections = listOf(Selection.fromPositions(TextPosition(0, 0), TextPosition(1, 5))),
            config = EditorConfig(tabSize = 4, useSoftTabs = true)
        )

        val tabCmd = EditReducer.reduce(state, EditAction.Tab).command as EditorCommand.ApplyText
        val tabBatch = tabCmd.operation as TextOperation.Data.Batch
        assertEquals(2, tabBatch.operations.size)

        val untabCmd = EditReducer.reduce(state, EditAction.Untab).command as EditorCommand.ApplyText
        val untabBatch = untabCmd.operation as TextOperation.Data.Batch
        val delLine1 = untabBatch.operations[0] as TextOperation.Data.Single.Delete
        val delLine0 = untabBatch.operations[1] as TextOperation.Data.Single.Delete

        assertEquals(TextRange(TextPosition(1, 0), TextPosition(1, 4)), delLine1.range)
        assertEquals(TextRange(TextPosition(0, 0), TextPosition(0, 2)), delLine0.range)
    }

    @Test
    fun `moveLineUp and moveLineDown swap adjacent lines and shift selections`() {
        val state = EditorState(
            snapshot = FakeTextSnapshot("first\nsecond\nthird"),
            selections = listOf(Selection.cursor(TextPosition(1, 2)))
        )

        val upCmd = EditReducer.reduce(state, EditAction.MoveLineUp).command as EditorCommand.ApplyText
        val upReplace = upCmd.operation as TextOperation.Data.Single.Replace
        assertEquals("second\nfirst", upReplace.text)
        assertEquals(TextRange(TextPosition(0, 0), TextPosition(1, 6)), upReplace.range)
        assertEquals(listOf(Selection.cursor(TextPosition(0, 2))), upCmd.targetSelections)

        val downCmd = EditReducer.reduce(state, EditAction.MoveLineDown).command as EditorCommand.ApplyText
        val downReplace = downCmd.operation as TextOperation.Data.Single.Replace
        assertEquals("third\nsecond", downReplace.text)
        assertEquals(TextRange(TextPosition(1, 0), TextPosition(2, 5)), downReplace.range)
        assertEquals(listOf(Selection.cursor(TextPosition(2, 2))), downCmd.targetSelections)
    }

    @Test
    fun `joinLines trims leading whitespace and inserts single space`() {
        val state = EditorState(
            snapshot = FakeTextSnapshot("val a =\n    42"), selections = listOf(Selection.cursor(TextPosition(0, 3)))
        )

        val cmd = EditReducer.reduce(state, EditAction.JoinLines).command as EditorCommand.ApplyText
        val replace = cmd.operation as TextOperation.Data.Single.Replace

        assertEquals("val a = 42", replace.text)
        assertEquals(TextRange(TextPosition(0, 0), TextPosition(1, 6)), replace.range)
    }

    @Test
    fun `wordDeleteLeft and wordDeleteRight delete word boundaries`() {
        val snapshot = FakeTextSnapshot("hello   world")

        val leftState = EditorState(snapshot, selections = listOf(Selection.cursor(TextPosition(0, 13))))
        val leftOp = (EditReducer.reduce(
            leftState, EditAction.WordDeleteLeft
        ).command as EditorCommand.ApplyText).operation as TextOperation.Data.Single.Delete
        assertEquals(TextRange(TextPosition(0, 8), TextPosition(0, 13)), leftOp.range)

        val rightState = EditorState(snapshot, selections = listOf(Selection.cursor(TextPosition(0, 0))))
        val rightOp = (EditReducer.reduce(
            rightState, EditAction.WordDeleteRight
        ).command as EditorCommand.ApplyText).operation as TextOperation.Data.Single.Delete
        assertEquals(TextRange(TextPosition(0, 0), TextPosition(0, 5)), rightOp.range)
    }
}