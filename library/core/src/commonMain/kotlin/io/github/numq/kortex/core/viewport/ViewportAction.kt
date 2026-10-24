package io.github.numq.kortex.core.viewport

import io.github.numq.kortex.core.EditorAction

/**
 * Actions modifying viewport folding state.
 */
sealed interface ViewportAction : EditorAction {
    data class UpdateCollapsedLines(val lines: Set<Int>) : ViewportAction
    data class UpdateFoldingRegions(val ranges: List<IntRange>) : ViewportAction
    data class ToggleFolding(val line: Int) : ViewportAction
}