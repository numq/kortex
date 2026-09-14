package io.github.numq.kortex.core.viewport

class FoldMap private constructor(
    val totalDocumentLines: Int,
    private val intervals: List<Interval>,
    val totalHiddenLines: Int,
) {
    internal data class Interval(
        val startLine: Int,
        val endLine: Int,
        val hiddenCount: Int,
        val cumulativeHiddenBefore: Int,
        val visualStartThreshold: Int,
    )

    val totalVisualLines = (totalDocumentLines - totalHiddenLines).coerceAtLeast(1)

    val activeRanges = intervals.map { interval ->
        interval.startLine..interval.endLine
    }

    fun isLineHidden(documentLine: Int) = when {
        intervals.isEmpty() || documentLine < 0 || documentLine >= totalDocumentLines -> false

        else -> {
            val idx = findIntervalIndexForDocumentLine(documentLine)

            idx in intervals.indices && documentLine in intervals[idx].startLine..intervals[idx].endLine
        }
    }

    fun visualToDocumentLine(visualLine: Int) = when {
        intervals.isEmpty() -> visualLine.coerceIn(0, (totalDocumentLines - 1).coerceAtLeast(0))

        visualLine <= 0 -> 0

        visualLine >= totalVisualLines -> (totalDocumentLines - 1).coerceAtLeast(0)

        else -> {
            var low = 0

            var high = intervals.size - 1

            var resultIndex = intervals.size

            while (low <= high) {
                val mid = (low + high) ushr 1

                when {
                    visualLine < intervals[mid].visualStartThreshold -> {
                        resultIndex = mid

                        high = mid - 1
                    }

                    else -> low = mid + 1
                }
            }

            val cumulativeHidden = when {
                resultIndex < intervals.size -> intervals[resultIndex].cumulativeHiddenBefore

                else -> totalHiddenLines
            }

            (visualLine + cumulativeHidden).coerceIn(0, (totalDocumentLines - 1).coerceAtLeast(0))
        }
    }

    fun documentToVisualLine(documentLine: Int): Int {
        if (intervals.isEmpty()) return documentLine.coerceIn(0, (totalVisualLines - 1).coerceAtLeast(0))

        val clampedDocLine = documentLine.coerceIn(0, (totalDocumentLines - 1).coerceAtLeast(0))

        var low = 0

        var high = intervals.size - 1

        var insideInterval: Interval? = null

        while (low <= high) {
            val mid = (low + high) ushr 1

            val interval = intervals[mid]

            when {
                clampedDocLine < interval.startLine -> high = mid - 1

                clampedDocLine > interval.endLine -> low = mid + 1

                else -> {
                    insideInterval = interval

                    break
                }
            }
        }

        if (insideInterval != null) {
            val visiblePrecedingDocLine = (insideInterval.startLine - 1).coerceAtLeast(0)

            return documentToVisualLine(visiblePrecedingDocLine)
        }

        val cumulativeHidden = when {
            high >= 0 -> intervals[high].cumulativeHiddenBefore + intervals[high].hiddenCount

            else -> 0
        }

        return (clampedDocLine - cumulativeHidden).coerceIn(0, (totalVisualLines - 1).coerceAtLeast(0))
    }

    private fun findIntervalIndexForDocumentLine(documentLine: Int): Int {
        var low = 0

        var high = intervals.size - 1

        while (low <= high) {
            val mid = (low + high) ushr 1

            val interval = intervals[mid]

            when {
                documentLine < interval.startLine -> high = mid - 1

                documentLine > interval.endLine -> low = mid + 1

                else -> return mid
            }
        }

        return -1
    }

    companion object {
        val EMPTY = FoldMap(totalDocumentLines = 1, intervals = emptyList(), totalHiddenLines = 0)

        fun of(
            totalLines: Int,
            collapsedRanges: List<IntRange>,
            collapsedLines: Set<Int>,
        ): FoldMap {
            if (totalLines <= 0) return EMPTY

            if (collapsedRanges.isEmpty() || collapsedLines.isEmpty()) {
                return FoldMap(totalDocumentLines = totalLines, intervals = emptyList(), totalHiddenLines = 0)
            }

            val rawRanges = ArrayList<IntRange>()

            for (range in collapsedRanges) {
                if (range.first in collapsedLines && range.last > range.first) {
                    val start = (range.first + 1).coerceIn(0, totalLines - 1)

                    val end = range.last.coerceIn(0, totalLines - 1)

                    if (start <= end) {
                        rawRanges.add(start..end)
                    }
                }
            }

            if (rawRanges.isEmpty()) {
                return FoldMap(totalDocumentLines = totalLines, intervals = emptyList(), totalHiddenLines = 0)
            }

            rawRanges.sortBy { range ->
                range.first
            }

            val merged = ArrayList<IntRange>()

            var currentStart = rawRanges[0].first

            var currentEnd = rawRanges[0].last

            for (i in 1 until rawRanges.size) {
                val r = rawRanges[i]

                when {
                    r.first <= currentEnd + 1 && r.last > currentEnd -> {
                        currentEnd = r.last
                    }

                    else -> {
                        merged.add(currentStart..currentEnd)

                        currentStart = r.first

                        currentEnd = r.last
                    }
                }
            }

            merged.add(currentStart..currentEnd)

            var cumulative = 0

            val intervals = ArrayList<Interval>(merged.size)

            for (r in merged) {
                val count = r.last - r.first + 1

                intervals.add(
                    Interval(
                        startLine = r.first,
                        endLine = r.last,
                        hiddenCount = count,
                        cumulativeHiddenBefore = cumulative,
                        visualStartThreshold = r.first - cumulative
                    )
                )

                cumulative += count
            }

            return FoldMap(
                totalDocumentLines = totalLines, intervals = intervals, totalHiddenLines = cumulative
            )
        }
    }
}