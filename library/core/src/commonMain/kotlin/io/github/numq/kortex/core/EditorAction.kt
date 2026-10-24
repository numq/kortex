package io.github.numq.kortex.core

/**
 * Marker interface for all intents and actions processed by the core editor state machine.
 */
interface EditorAction {
    /** Updates the editor configuration parameters. */
    data class UpdateConfig(val config: EditorConfig) : EditorAction
}