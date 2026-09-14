package io.github.numq.kortex.code

import io.github.numq.kortex.code.analysis.Analysis
import io.github.numq.kortex.code.analysis.CodeSuggestion
import io.github.numq.kortex.code.syntax.Syntax
import io.github.numq.kortex.core.EditorAction
import io.github.numq.krope.text.TextPosition

sealed interface CodeEditorAction {
    data class Core(val action: EditorAction) : CodeEditorAction

    sealed interface Edit : CodeEditorAction {
        data class ToggleComment(val prefix: String) : Edit
    }

    sealed interface External : CodeEditorAction {
        data class UpdateSyntax(val syntax: Syntax?) : External

        data class UpdateAnalysis(val analysis: Analysis?) : External
    }

    sealed interface Completion : CodeEditorAction {
        data class Show(val suggestions: List<CodeSuggestion>, val position: TextPosition) : Completion

        data object SelectNext : Completion

        data object SelectPrevious : Completion

        data object Apply : Completion

        data object Dismiss : Completion
    }
}