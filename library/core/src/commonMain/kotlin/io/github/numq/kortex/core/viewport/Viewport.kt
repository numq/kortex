package io.github.numq.kortex.core.viewport

import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextSnapshot
import kotlin.math.ceil
import kotlin.math.floor

data class Viewport(
    val width: Float,
    val height: Float,
    val visibleLines: IntRange,
    val viewportLines: List<ViewportLine>,
) {
    fun findLineAtY(y: Float) = viewportLines.find { viewportLine ->
        y >= viewportLine.y && y <= (viewportLine.y + viewportLine.height)
    }

    fun resolvePosition(
        x: Float,
        y: Float,
        snapshot: TextSnapshot,
        charWidth: Float,
        columnResolver: ((line: Int, localX: Float) -> Int)? = null,
    ): TextPosition {
        val line = findLineAtY(y)

        if (line == null) {
            val firstY = viewportLines.firstOrNull()?.y ?: 0f

            return when {
                y < firstY -> TextPosition.ZERO

                else -> {
                    val lastLine = (snapshot.lines - 1).coerceAtLeast(
                        0
                    )
                    TextPosition(line = lastLine, column = snapshot.getLineLength(lastLine))
                }
            }
        }

        val column = columnResolver?.invoke(line.line, x) ?: when {
            charWidth > 0f -> (x / charWidth).toInt().coerceIn(0, snapshot.getLineLength(line.line))

            else -> 0
        }

        return TextPosition(line = line.line, column = column)
    }

    companion object {
        val EMPTY = Viewport(
            width = 0f, height = 0f, visibleLines = IntRange.EMPTY, viewportLines = emptyList()
        )

        fun of(
            snapshot: TextSnapshot,
            foldMap: FoldMap,
            width: Float,
            height: Float,
            scrollY: Float,
            ascent: Float,
            textHeight: Float,
            lineHeight: Float,
        ): Viewport = when {
            width <= 0f || height <= 0f || lineHeight <= 0f -> EMPTY

            else -> {
                val snappedLineHeight = ceil(lineHeight)

                val totalVisualLines = foldMap.totalVisualLines

                val effectiveScrollY = maxOf(0f, scrollY)

                val startVisualLine = floor(effectiveScrollY / snappedLineHeight).toInt()
                    .coerceIn(0, (totalVisualLines - 1).coerceAtLeast(0))

                val visualLinesInViewport = ceil(height / snappedLineHeight).toInt()

                val endVisualLine = (startVisualLine + visualLinesInViewport + 1).coerceAtMost(totalVisualLines - 1)

                val visibleVisualLinesRange = startVisualLine..endVisualLine

                val viewportLines = visibleVisualLinesRange.map { visualLineIndex ->
                    val documentLineIndex = foldMap.visualToDocumentLine(visualLineIndex)

                    val text = snapshot.getLineText(line = documentLineIndex)

                    val lineTop = floor((visualLineIndex * snappedLineHeight) - effectiveScrollY)

                    val leading = snappedLineHeight - textHeight

                    val textBaselineY = lineTop + (leading / 2f) - ascent

                    ViewportLine(
                        line = documentLineIndex,
                        x = 0f,
                        y = lineTop,
                        width = width,
                        height = snappedLineHeight,
                        text = text,
                        textBaselineY = textBaselineY
                    )
                }

                val visibleLines = when {
                    viewportLines.isEmpty() -> IntRange.EMPTY

                    else -> viewportLines.first().line..viewportLines.last().line
                }

                Viewport(
                    width = width, height = height, visibleLines = visibleLines, viewportLines = viewportLines
                )
            }
        }
    }
}