package io.github.numq.kortex.core.viewport

import io.github.numq.kortex.core.EditorResult
import io.github.numq.kortex.core.EditorState

internal object ViewportReducer {
    fun reduce(state: EditorState, action: ViewportAction) = when (action) {
        is ViewportAction.UpdateCollapsedLines -> EditorResult(state.copy(collapsedLines = action.lines))

        is ViewportAction.UpdateFoldingRegions -> EditorResult(state.copy(collapsedRanges = action.ranges))

        is ViewportAction.ToggleFolding -> {
            val newCollapsedLines = when {
                state.collapsedLines.contains(action.line) -> state.collapsedLines - action.line

                else -> state.collapsedLines + action.line
            }

            EditorResult(state.copy(collapsedLines = newCollapsedLines))
        }
    }
}