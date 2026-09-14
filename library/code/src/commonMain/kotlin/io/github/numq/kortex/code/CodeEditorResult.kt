package io.github.numq.kortex.code

data class CodeEditorResult(
    val state: CodeEditorState,
    val command: CodeEditorCommand? = null,
)