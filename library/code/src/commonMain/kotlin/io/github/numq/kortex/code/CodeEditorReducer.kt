package io.github.numq.kortex.code

import io.github.numq.kortex.code.analysis.CodeSuggestion
import io.github.numq.kortex.code.comment.CommentOperations
import io.github.numq.kortex.code.syntax.FoldingRegion
import io.github.numq.kortex.core.EditorReducer
import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.selection.SelectionAction
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange

/**
 * Pure state reducer for [CodeEditorState] processing [CodeEditorAction] inputs.
 */
object CodeEditorReducer {
    private fun applySuggestion(
        state: CodeEditorState,
        anchorPosition: TextPosition,
        item: CodeSuggestion?,
    ): CodeEditorResult = when (item) {
        null -> CodeEditorResult(state.copy(completion = CodeEditorState.CompletionState.Hidden))

        else -> {
            val currentCaret = state.editor.primarySelection.caret
            val operation = when {
                item.range != null -> TextOperation.Data.Single.Replace(item.range, item.text)

                currentCaret.line == anchorPosition.line && currentCaret.column > anchorPosition.column -> {
                    TextOperation.Data.Single.Replace(
                        range = TextRange(anchorPosition, currentCaret),
                        text = item.text,
                    )
                }

                else -> TextOperation.Data.Single.Insert(anchorPosition, item.text)
            }

            CodeEditorResult(
                state = state.copy(completion = CodeEditorState.CompletionState.Hidden),
                command = CodeEditorCommand.ApplyText(operation, canMerge = false),
            )
        }
    }

    /**
     * Transforms [state] by applying [action] and producing an updated state with an optional side-effect command.
     */
    fun reduce(state: CodeEditorState, action: CodeEditorAction): CodeEditorResult = when (action) {
        is CodeEditorAction.Core -> {
            val (updatedEditor, coreCommand) = EditorReducer.reduce(state.editor, action.action)

            val shouldDismissOverlays =
                action.action is CaretAction.Move || action.action is SelectionAction.Clear || updatedEditor.caret != state.editor.caret

            val updatedCompletion = when {
                shouldDismissOverlays -> CodeEditorState.CompletionState.Hidden
                else -> state.completion
            }

            val updatedDocumentation = when {
                shouldDismissOverlays -> null
                else -> state.documentation
            }

            CodeEditorResult(
                state = state.copy(
                    editor = updatedEditor,
                    completion = updatedCompletion,
                    documentation = updatedDocumentation,
                ),
                command = coreCommand?.let(CodeEditorCommand::Core),
            )
        }

        is CodeEditorAction.Edit.ToggleComment -> {
            val operation = CommentOperations.calculateToggleComment(state.editor, action.prefix)

            CodeEditorResult(
                state = state,
                command = CodeEditorCommand.ApplyText(operation, canMerge = false),
            )
        }

        is CodeEditorAction.Completion.Apply -> {
            val visible = state.completion as? CodeEditorState.CompletionState.Visible
            applySuggestion(state, visible?.anchorPosition ?: state.caret.position, visible?.selectedItem)
        }

        is CodeEditorAction.Completion.ApplyAt -> {
            val visible = state.completion as? CodeEditorState.CompletionState.Visible
            val item = visible?.suggestions?.getOrNull(action.index)
            applySuggestion(state, visible?.anchorPosition ?: state.caret.position, item)
        }

        is CodeEditorAction.Completion.Dismiss -> CodeEditorResult(
            state.copy(completion = CodeEditorState.CompletionState.Hidden)
        )

        is CodeEditorAction.Completion.Show -> {
            val nextCompletion = when {
                action.suggestions.isEmpty() -> CodeEditorState.CompletionState.Hidden

                else -> CodeEditorState.CompletionState.Visible(
                    suggestions = action.suggestions,
                    selectedIndex = 0,
                    anchorPosition = action.position,
                )
            }

            CodeEditorResult(state.copy(completion = nextCompletion))
        }

        is CodeEditorAction.Completion.SelectNext -> when (val visible =
            state.completion as? CodeEditorState.CompletionState.Visible) {
            null -> CodeEditorResult(state)

            else -> {
                val nextIdx = (visible.selectedIndex + 1).coerceAtMost(visible.suggestions.lastIndex)

                CodeEditorResult(state.copy(completion = visible.copy(selectedIndex = nextIdx)))
            }
        }

        is CodeEditorAction.Completion.SelectPrevious -> when (val visible =
            state.completion as? CodeEditorState.CompletionState.Visible) {
            null -> CodeEditorResult(state)

            else -> {
                val prevIdx = (visible.selectedIndex - 1).coerceAtLeast(0)

                CodeEditorResult(state.copy(completion = visible.copy(selectedIndex = prevIdx)))
            }
        }

        is CodeEditorAction.External.UpdateSyntax -> {
            val regions = action.syntax?.foldingRegions.orEmpty()

            val availableRanges = regions.map { region ->
                region.range.start.line..region.range.end.line
            }

            val validFoldStartLines = availableRanges.map(IntRange::first).toSet()

            val previousFoldStartLines = state.syntax?.foldingRegions?.map { region ->
                region.range.start.line
            }?.toSet().orEmpty()

            val newlyCollapsed =
                regions.filterIsInstance<FoldingRegion.Collapsed>().map { region -> region.range.start.line }
                    .filter { line -> line !in previousFoldStartLines }.toSet()

            val preservedCollapsedLines = state.editor.collapsedLines.intersect(validFoldStartLines)

            val updatedEditor = state.editor.copy(
                collapsedRanges = availableRanges,
                collapsedLines = preservedCollapsedLines + newlyCollapsed,
            )

            CodeEditorResult(state.copy(editor = updatedEditor, syntax = action.syntax))
        }

        is CodeEditorAction.External.UpdateAnalysis -> CodeEditorResult(state.copy(analysis = action.analysis))

        is CodeEditorAction.External.UpdateDocumentation -> CodeEditorResult(
            state.copy(documentation = action.documentation)
        )
    }
}