package io.github.numq.kortex.code

import io.github.numq.kortex.code.comment.CommentOperations
import io.github.numq.kortex.code.syntax.FoldingRegion
import io.github.numq.kortex.core.EditorReducer
import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.selection.SelectionAction
import io.github.numq.krope.text.TextOperation

object CodeEditorReducer {
    fun reduce(state: CodeEditorState, action: CodeEditorAction) = when (action) {
        is CodeEditorAction.Core -> {
            val (updatedEditor, coreCommand) = EditorReducer.reduce(state.editor, action.action)

            val updatedCompletion = when (action.action) {
                is CaretAction.Move, is SelectionAction.Clear -> CodeEditorState.CompletionState.Hidden

                else -> state.completion
            }

            CodeEditorResult(
                state = state.copy(editor = updatedEditor, completion = updatedCompletion),
                command = coreCommand?.let(CodeEditorCommand::Core)
            )
        }

        is CodeEditorAction.Edit.ToggleComment -> {
            val operation = CommentOperations.calculateToggleComment(state.editor, action.prefix)

            CodeEditorResult(
                state = state, command = CodeEditorCommand.ApplyText(operation, canMerge = false)
            )
        }

        is CodeEditorAction.Completion.Apply -> {
            val visible = state.completion as? CodeEditorState.CompletionState.Visible

            when (val item = visible?.selectedItem) {
                null -> CodeEditorResult(state.copy(completion = CodeEditorState.CompletionState.Hidden))

                else -> {
                    val operation = when (item.range) {
                        null -> TextOperation.Data.Single.Insert(visible.anchorPosition, item.text)

                        else -> TextOperation.Data.Single.Replace(item.range, item.text)
                    }

                    CodeEditorResult(
                        state = state.copy(completion = CodeEditorState.CompletionState.Hidden),
                        command = CodeEditorCommand.ApplyText(operation, canMerge = false)
                    )
                }
            }
        }

        is CodeEditorAction.Completion.Dismiss -> CodeEditorResult(state.copy(completion = CodeEditorState.CompletionState.Hidden))

        is CodeEditorAction.Completion.Show -> {
            val nextCompletion = when {
                action.suggestions.isEmpty() -> CodeEditorState.CompletionState.Hidden

                else -> CodeEditorState.CompletionState.Visible(
                    suggestions = action.suggestions, selectedIndex = 0, anchorPosition = action.position
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

            val initiallyCollapsed = regions.filterIsInstance<FoldingRegion.Collapsed>().map { region ->
                region.range.start.line
            }.toSet()

            val updatedEditor = state.editor.copy(
                collapsedRanges = availableRanges, collapsedLines = state.editor.collapsedLines + initiallyCollapsed
            )

            CodeEditorResult(state.copy(editor = updatedEditor, syntax = action.syntax))
        }

        is CodeEditorAction.External.UpdateAnalysis -> CodeEditorResult(state.copy(analysis = action.analysis))
    }
}