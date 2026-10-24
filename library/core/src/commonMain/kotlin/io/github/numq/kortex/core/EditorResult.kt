package io.github.numq.kortex.core

/**
 * Result returned by [EditorReducer] containing the next [EditorState] and an optional [EditorCommand].
 */
data class EditorResult(val state: EditorState, val command: EditorCommand? = null)