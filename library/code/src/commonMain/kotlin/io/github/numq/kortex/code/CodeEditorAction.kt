package io.github.numq.kortex.code

import io.github.numq.kortex.code.analysis.Analysis
import io.github.numq.kortex.code.analysis.CodeDocumentation
import io.github.numq.kortex.code.analysis.CodeSuggestion
import io.github.numq.kortex.code.syntax.Syntax
import io.github.numq.kortex.core.EditorAction
import io.github.numq.krope.text.TextPosition

/**
 * Actions accepted by the code editor state reducer and [CodeEditorEngine].
 */
sealed interface CodeEditorAction {
    /** Wraps a low-level core [EditorAction]. */
    data class Core(val action: EditorAction) : CodeEditorAction

    /** Code editing mutations. */
    sealed interface Edit : CodeEditorAction {
        /**
         * Toggles line comment prefix across all selected or caret lines.
         *
         * @property prefix Line comment prefix string (e.g. `"// "` or `"# "`).
         */
        data class ToggleComment(val prefix: String) : Edit
    }

    /** Actions triggered by external asynchronous background services. */
    sealed interface External : CodeEditorAction {
        /** Updates syntax tokens, folding ranges, and occurrences. */
        data class UpdateSyntax(val syntax: Syntax?) : External

        /** Updates diagnostic issues. */
        data class UpdateAnalysis(val analysis: Analysis?) : External

        /** Updates symbol documentation. */
        data class UpdateDocumentation(val documentation: CodeDocumentation?) : External
    }

    /** Actions controlling the code completion dropdown. */
    sealed interface Completion : CodeEditorAction {
        /** Displays completion popup with [suggestions] anchored at [position]. */
        data class Show(val suggestions: List<CodeSuggestion>, val position: TextPosition) : Completion

        /** Selects the next suggestion in the active completion popup. */
        data object SelectNext : Completion

        /** Selects the previous suggestion in the active completion popup. */
        data object SelectPrevious : Completion

        /** Applies the currently highlighted suggestion to the buffer. */
        data object Apply : Completion

        /** Applies the suggestion at [index] to the buffer. */
        data class ApplyAt(val index: Int) : Completion

        /** Dismisses the active completion popup without applying changes. */
        data object Dismiss : Completion
    }
}