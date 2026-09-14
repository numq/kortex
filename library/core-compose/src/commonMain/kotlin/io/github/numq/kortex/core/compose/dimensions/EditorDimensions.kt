package io.github.numq.kortex.core.compose.dimensions

data class EditorDimensions(
    val caretWidth: Float = 2f,
    val gutterPaddingStart: Float = 8f,
    val gutterPaddingEnd: Float = 4f,
    val gutterGap: Float = 4f,
    val editorPaddingStart: Float = 4f,
    val editorPaddingEnd: Float = 64f,
    val contentPaddingBottom: Float = 128f,
)