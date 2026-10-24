package io.github.numq.kortex.code

import io.github.numq.kortex.code.analysis.Analysis
import io.github.numq.kortex.code.analysis.CodeDocumentation
import io.github.numq.kortex.code.analysis.CodeSuggestion
import io.github.numq.kortex.code.syntax.Syntax
import io.github.numq.kortex.core.EditorState
import io.github.numq.krope.text.TextPosition

/**
 * Unified state of the code editor, combining core text editing with syntax highlighting,
 * background diagnostics, documentation, and completion popup state.
 *
 * @property editor The underlying core editor state.
 * @property syntax Current syntax highlighting, occurrence, and folding metadata.
 * @property analysis Current diagnostic issues.
 * @property documentation Current hover or symbol documentation, if resolved.
 * @property completion State of the interactive code completion popup.
 */
data class CodeEditorState(
    val editor: EditorState,
    val syntax: Syntax? = null,
    val analysis: Analysis? = null,
    val documentation: CodeDocumentation? = null,
    val completion: CompletionState = CompletionState.Hidden,
) {
    val snapshot get() = editor.snapshot
    val selections get() = editor.selections
    val primarySelection get() = editor.primarySelection
    val caret get() = editor.caret
    val foldMap get() = editor.foldMap

    /**
     * Represents the display state of the code completion popup.
     */
    sealed interface CompletionState {
        /** Completion popup is closed. */
        data object Hidden : CompletionState

        /**
         * Completion popup is active and visible.
         *
         * @property suggestions The list of available suggestions.
         * @property selectedIndex The 0-based index of the currently highlighted suggestion.
         * @property anchorPosition Document position where the completion was originally invoked.
         */
        data class Visible(
            val suggestions: List<CodeSuggestion>,
            val selectedIndex: Int = 0,
            val anchorPosition: TextPosition,
        ) : CompletionState {
            val selectedItem: CodeSuggestion? get() = suggestions.getOrNull(selectedIndex)
        }
    }
}