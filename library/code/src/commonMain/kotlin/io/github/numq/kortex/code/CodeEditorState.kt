package io.github.numq.kortex.code

import io.github.numq.kortex.code.analysis.Analysis
import io.github.numq.kortex.code.analysis.CodeSuggestion
import io.github.numq.kortex.code.syntax.Syntax
import io.github.numq.kortex.core.EditorState
import io.github.numq.krope.text.TextPosition

data class CodeEditorState(
    val editor: EditorState,
    val syntax: Syntax? = null,
    val analysis: Analysis? = null,
    val completion: CompletionState = CompletionState.Hidden,
) {
    val snapshot get() = editor.snapshot

    val selections get() = editor.selections

    val primarySelection get() = editor.primarySelection

    val caret get() = editor.caret

    val foldMap get() = editor.foldMap

    sealed interface CompletionState {
        data object Hidden : CompletionState

        data class Visible(
            val suggestions: List<CodeSuggestion>,
            val selectedIndex: Int = 0,
            val anchorPosition: TextPosition,
        ) : CompletionState {
            val selectedItem: CodeSuggestion? get() = suggestions.getOrNull(selectedIndex)
        }
    }
}