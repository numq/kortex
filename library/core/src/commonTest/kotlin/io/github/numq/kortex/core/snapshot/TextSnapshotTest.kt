package io.github.numq.kortex.core.snapshot

import io.github.numq.kortex.core.text.offsetOf
import io.github.numq.kortex.core.text.positionOf
import io.github.numq.krope.text.TextLineEnding
import io.github.numq.krope.text.TextPosition
import kotlin.test.Test
import kotlin.test.assertEquals

class TextSnapshotTest {
    @Test
    fun `offsetOf and positionOf are symmetric for LF and CRLF`() {
        val lfSnapshot = FakeTextSnapshot("abc\ndefg\nhi", lineEnding = TextLineEnding.LF)
        val pos = TextPosition(1, 2)
        val offset = lfSnapshot.offsetOf(pos)
        assertEquals(6, offset)
        assertEquals(pos, lfSnapshot.positionOf(offset))

        val crlfSnapshot = FakeTextSnapshot("abc\ndefg\nhi", lineEnding = TextLineEnding.CRLF)
        val crlfOffset = crlfSnapshot.offsetOf(pos)
        assertEquals(7, crlfOffset)
        assertEquals(pos, crlfSnapshot.positionOf(crlfOffset))
    }
}