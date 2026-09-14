package io.github.numq.kortex.core.text

import io.github.numq.krope.text.TextLineEnding
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextSnapshot

fun TextSnapshot.getDelimiterLength(): Int = when (lineEnding) {
    TextLineEnding.CRLF -> 2

    else -> 1
}

fun TextSnapshot.offsetOf(position: TextPosition): Int {
    val delimiter = getDelimiterLength()

    var offset = 0

    val targetLine = position.line.coerceIn(0, (lines - 1).coerceAtLeast(0))

    for (line in 0 until targetLine) {
        offset += getLineLength(line) + delimiter
    }

    return offset + position.column.coerceIn(0, getLineLength(targetLine))
}

fun TextSnapshot.positionOf(offset: Int): TextPosition {
    val delimiter = getDelimiterLength()

    var remaining = offset.coerceAtLeast(0)

    for (line in 0 until lines) {
        val lineLen = getLineLength(line)

        val fullLineLen = lineLen + delimiter

        if (remaining <= lineLen) {
            return TextPosition(line, remaining)
        }

        if (remaining < fullLineLen) {
            return TextPosition(line, lineLen)
        }

        remaining -= fullLineLen
    }

    return lastPosition
}