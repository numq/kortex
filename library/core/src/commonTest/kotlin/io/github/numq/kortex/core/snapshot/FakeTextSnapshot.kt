package io.github.numq.kortex.core.snapshot

import io.github.numq.krope.core.Encoding
import io.github.numq.krope.text.*

class FakeTextSnapshot(
    override val text: String,
    override val lineEnding: TextLineEnding = TextLineEnding.LF,
    override val revision: TextRevision = TextRevision(1L),
    override val encoding: Encoding = Encoding.UTF8,
) : TextSnapshot {
    private val lineList: List<String> = text.split("\n")

    override val lines: Int get() = lineList.size

    override val maxLineLength: Int get() = lineList.maxOfOrNull { it.length } ?: 0

    override val lastPosition: TextPosition
        get() {
            val lastIdx = (lineList.size - 1).coerceAtLeast(0)
            return TextPosition(lastIdx, lineList[lastIdx].length)
        }

    override fun getLineText(line: Int): String = lineList.getOrElse(line) { "" }

    override fun getLineLength(line: Int): Int = getLineText(line).length

    override fun getTextInRange(range: TextRange): String {
        if (range.isEmpty) return ""
        val start = range.start
        val end = range.end
        if (start.line == end.line) {
            return getLineText(start.line).substring(start.column, end.column)
        }
        return buildString {
            append(getLineText(start.line).substring(start.column))
            for (line in (start.line + 1) until end.line) {
                append("\n")
                append(getLineText(line))
            }
            append("\n")
            append(getLineText(end.line).substring(0, end.column))
        }
    }

    override fun isValidPosition(position: TextPosition): Boolean =
        position.line in 0 until lines && position.column in 0..getLineLength(position.line)

    override fun getBytePosition(position: TextPosition): Int? = null

    override fun getTextPosition(bytePosition: Int): TextPosition? = null
}