package io.github.numq.kortex.core.selection

import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class SelectionTest {
    @Test
    fun `normalize merges overlapping and adjacent selections`() {
        val sel1 = Selection.fromPositions(TextPosition(0, 0), TextPosition(0, 5))
        val sel2 = Selection.fromPositions(TextPosition(0, 4), TextPosition(0, 10))
        val sel3 = Selection.fromPositions(TextPosition(1, 0), TextPosition(1, 4))

        val normalized = Selection.normalize(listOf(sel2, sel1, sel3))

        assertEquals(2, normalized.size)
        assertEquals(TextRange(TextPosition(0, 0), TextPosition(0, 10)), normalized[0].range)
        assertEquals(TextRange(TextPosition(1, 0), TextPosition(1, 4)), normalized[1].range)
    }

    @Test
    fun `normalize merges duplicate zero-width cursors`() {
        val cur1 = Selection.cursor(TextPosition(0, 5))
        val cur2 = Selection.cursor(TextPosition(0, 5))
        val cur3 = Selection.cursor(TextPosition(0, 10))

        val normalized = Selection.normalize(listOf(cur1, cur2, cur3))

        assertEquals(2, normalized.size)
        assertEquals(TextPosition(0, 5), normalized[0].caret)
        assertEquals(TextPosition(0, 10), normalized[1].caret)
    }

    @Test
    fun `selection direction detects forward and backward orientation`() {
        val forward = Selection.fromPositions(TextPosition(0, 2), TextPosition(0, 8))
        val backward = Selection.fromPositions(TextPosition(0, 8), TextPosition(0, 2))

        assertEquals(SelectionDirection.FORWARD, forward.direction)
        assertEquals(SelectionDirection.BACKWARD, backward.direction)
        assertEquals(forward.range, backward.range)
    }
}