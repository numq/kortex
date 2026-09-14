package io.github.numq.kortex.core

interface EditorAction {
    data class UpdateConfig(val config: EditorConfig) : EditorAction
}