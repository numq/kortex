package io.github.numq.kortex.core

import io.github.numq.kortex.core.caret.Caret
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.viewport.FoldMap
import io.github.numq.krope.text.TextSnapshot

data class EditorState(
    val snapshot: TextSnapshot,
    val selections: List<Selection> = listOf(Selection.ZERO),
    val collapsedLines: Set<Int> = emptySet(),
    val collapsedRanges: List<IntRange> = emptyList(),
    val config: EditorConfig = EditorConfig(),
) {
    val primarySelection: Selection get() = selections.lastOrNull() ?: Selection.ZERO

    val caret: Caret get() = Caret(primarySelection.caret)

    val selection: Selection get() = primarySelection

    val stickyColumn: Int get() = primarySelection.stickyColumn

    val foldMap: FoldMap
        get() = FoldMap.of(
            totalLines = snapshot.lines, collapsedRanges = collapsedRanges, collapsedLines = collapsedLines
        )

    val activeCollapsedRanges: List<IntRange> get() = foldMap.activeRanges

    constructor(
        snapshot: TextSnapshot,
        caret: Caret,
        selection: Selection = Selection.EMPTY,
        stickyColumn: Int = caret.position.column,
        collapsedLines: Set<Int> = emptySet(),
        collapsedRanges: List<IntRange> = emptyList(),
        config: EditorConfig = EditorConfig()
    ) : this(
        snapshot = snapshot, selections = listOf(
            when {
                selection.isNotEmpty -> selection.copy(stickyColumn = stickyColumn)

                else -> Selection.cursor(caret.position, stickyColumn)
            }
        ), collapsedLines = collapsedLines, collapsedRanges = collapsedRanges, config = config
    )
}