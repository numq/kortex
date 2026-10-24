package io.github.numq.kortex.code

/**
 * Result emitted by [CodeEditorReducer].
 *
 * @property state The updated [CodeEditorState].
 * @property command An optional side effect command to be executed by [CodeEditorEngine].
 */
data class CodeEditorResult(
    val state: CodeEditorState,
    val command: CodeEditorCommand? = null,
)