package io.github.numq.kortex.code.compose.keymap

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import io.github.numq.kortex.code.CodeEditorAction
import io.github.numq.kortex.code.CodeEditorEngine
import io.github.numq.kortex.code.CodeEditorState
import io.github.numq.kortex.core.compose.keymap.DefaultEditorKeymap
import io.github.numq.kortex.core.compose.keymap.EditorKeymap
import io.github.numq.kortex.core.compose.keymap.buildEditorKeymap

object DefaultCodeEditorKeymap {
    fun create(
        codeEngine: CodeEditorEngine,
        commentPrefix: String = "// ",
        baseKeymap: EditorKeymap = DefaultEditorKeymap.create(),
    ): EditorKeymap = buildEditorKeymap {
        bindConditional(matcher = { key == Key.DirectionDown }, description = "Completion Next") {
            when (codeEngine.state.value.completion) {
                is CodeEditorState.CompletionState.Visible -> {
                    codeEngine.dispatch(CodeEditorAction.Completion.SelectNext)

                    true
                }

                else -> false
            }
        }

        bindConditional(matcher = { key == Key.DirectionUp }, description = "Completion Previous") {
            when (codeEngine.state.value.completion) {
                is CodeEditorState.CompletionState.Visible -> {
                    codeEngine.dispatch(CodeEditorAction.Completion.SelectPrevious)

                    true
                }

                else -> false
            }
        }

        bindConditional(
            matcher = { key == Key.Enter || key == Key.NumPadEnter || key == Key.Tab }, description = "Apply Completion"
        ) {
            when (codeEngine.state.value.completion) {
                is CodeEditorState.CompletionState.Visible -> {
                    codeEngine.dispatch(CodeEditorAction.Completion.Apply)

                    true
                }

                else -> false
            }
        }

        bindConditional(matcher = { key == Key.Escape }, description = "Dismiss Completion") {
            when (codeEngine.state.value.completion) {
                is CodeEditorState.CompletionState.Visible -> {
                    codeEngine.dispatch(CodeEditorAction.Completion.Dismiss)

                    true
                }

                else -> false
            }
        }

        bind(Key.Spacebar, primary = true, description = "Trigger Code Completion") {
            codeEngine.requestCompletion()
        }

        bind(Key.Slash, primary = true, requiresEditable = true, description = "Toggle Comment") {
            codeEngine.dispatch(CodeEditorAction.Edit.ToggleComment(commentPrefix))
        }

        include(baseKeymap)
    }
}