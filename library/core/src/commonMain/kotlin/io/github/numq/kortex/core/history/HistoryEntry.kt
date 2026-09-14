package io.github.numq.kortex.core.history

import io.github.numq.kortex.core.caret.Caret
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import kotlin.time.ComparableTimeMark

data class HistoryEntry(
    val forward: TextOperation.Data,
    val backward: TextOperation.Data,
    val selectionsBefore: List<Selection>,
    val selectionsAfter: List<Selection>,
    val timestamp: ComparableTimeMark,
    val canMerge: Boolean,
) {
    val caretBefore: Caret get() = Caret(selectionsBefore.lastOrNull()?.caret ?: TextPosition.ZERO)

    val selectionBefore: Selection get() = selectionsBefore.lastOrNull() ?: Selection.EMPTY

    val caretAfter: Caret get() = Caret(selectionsAfter.lastOrNull()?.caret ?: TextPosition.ZERO)

    val selectionAfter: Selection get() = selectionsAfter.lastOrNull() ?: Selection.EMPTY

    constructor(
        forward: TextOperation.Data,
        backward: TextOperation.Data,
        caretBefore: Caret,
        selectionBefore: Selection,
        caretAfter: Caret,
        selectionAfter: Selection,
        timestamp: ComparableTimeMark,
        canMerge: Boolean,
    ) : this(
        forward = forward,
        backward = backward,
        selectionsBefore = listOf(if (selectionBefore.isNotEmpty) selectionBefore else Selection.cursor(caretBefore.position)),
        selectionsAfter = listOf(if (selectionAfter.isNotEmpty) selectionAfter else Selection.cursor(caretAfter.position)),
        timestamp = timestamp,
        canMerge = canMerge
    )
}