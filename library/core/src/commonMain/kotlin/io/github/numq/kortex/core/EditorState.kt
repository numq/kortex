package io.github.numq.kortex.core

import io.github.numq.kortex.core.caret.Caret
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.kortex.core.viewport.FoldMap
import io.github.numq.krope.text.TextSnapshot

/**
 * Immutable snapshot of the core editor state at a specific point in time.
 *
 * @property snapshot The immutable text snapshot from the underlying rope buffer.
 * @property selections Normalized list of active cursor and range selections. Guaranteed to contain at least one element.
 * @property collapsedLines Set of 0-based document line indices where folding regions are currently collapsed.
 * @property collapsedRanges Available foldable line ranges in the document.
 * @property config Current editor configuration.
 */
data class EditorState(
    val snapshot: TextSnapshot,
    val selections: List<Selection> = listOf(Selection.ZERO),
    val collapsedLines: Set<Int> = emptySet(),
    val collapsedRanges: List<IntRange> = emptyList(),
    val config: EditorConfig = EditorConfig(),
) {
    /** The primary (most recently added or last) selection in [selections]. */
    val primarySelection: Selection get() = selections.lastOrNull() ?: Selection.ZERO

    /** The caret position of the [primarySelection]. */
    val caret: Caret get() = Caret(primarySelection.caret)

    /** Alias for [primarySelection]. */
    val selection: Selection get() = primarySelection

    /** The preferred horizontal column maintained across vertical caret movements. */
    val stickyColumn: Int get() = primarySelection.stickyColumn

    /** Precomputed mapping between document lines and visual lines accounting for active code folding. */
    val foldMap: FoldMap = FoldMap.of(
        totalLines = snapshot.lines, collapsedRanges = collapsedRanges, collapsedLines = collapsedLines
    )

    /** Currently active (collapsed) line ranges in the document. */
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