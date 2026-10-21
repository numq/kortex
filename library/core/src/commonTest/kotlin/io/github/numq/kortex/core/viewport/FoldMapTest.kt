package io.github.numq.kortex.core.viewport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FoldMapTest {
    @Test
    fun `empty fold map maps 1 to 1`() {
        val foldMap = FoldMap.of(
            totalLines = 10, collapsedRanges = emptyList(), collapsedLines = emptySet()
        )

        assertEquals(10, foldMap.totalVisualLines)
        for (i in 0 until 10) {
            assertEquals(i, foldMap.documentToVisualLine(i))
            assertEquals(i, foldMap.visualToDocumentLine(i))
            assertFalse(foldMap.isLineHidden(i))
        }
    }

    @Test
    fun `folding collapses lines after range start`() {
        val foldMap = FoldMap.of(
            totalLines = 10, collapsedRanges = listOf(2..5), collapsedLines = setOf(2)
        )

        assertEquals(7, foldMap.totalVisualLines)

        assertFalse(foldMap.isLineHidden(2))
        assertEquals(2, foldMap.documentToVisualLine(2))

        assertTrue(foldMap.isLineHidden(3))
        assertTrue(foldMap.isLineHidden(4))
        assertTrue(foldMap.isLineHidden(5))

        assertFalse(foldMap.isLineHidden(6))
        assertEquals(3, foldMap.documentToVisualLine(6))
        assertEquals(6, foldMap.visualToDocumentLine(3))
    }

    @Test
    fun `merges overlapping folding ranges`() {
        val foldMap = FoldMap.of(
            totalLines = 15, collapsedRanges = listOf(1..4, 3..7), collapsedLines = setOf(1, 3)
        )

        assertEquals(9, foldMap.totalVisualLines)
        assertEquals(1, foldMap.documentToVisualLine(1))
        assertEquals(2, foldMap.documentToVisualLine(8))
        assertEquals(8, foldMap.visualToDocumentLine(2))
    }
}