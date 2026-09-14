package io.github.numq.kortex.core.compose.layout

import androidx.compose.runtime.Immutable

@Immutable
data class EditorLayoutInfo(
    val viewportWidth: Float = 0f,
    val viewportHeight: Float = 0f,
    val isGutterVisible: Boolean = true,
    val gutterWidth: Float = 0f,
    val textAreaWidth: Float = (viewportWidth - gutterWidth).coerceAtLeast(0f),
    val textContentWidth: Float = 0f,
    val contentHeight: Float = 0f,
    val scrollX: Float = 0f,
    val scrollY: Float = 0f,
    val lineHeight: Float = 0f,
    val charWidth: Float = 0f,
    val visibleLinesRange: IntRange = IntRange.EMPTY,
) {
    val maxScrollX: Float
        get() = (textContentWidth - textAreaWidth).coerceAtLeast(0f)

    val maxScrollY: Float
        get() = (contentHeight - viewportHeight).coerceAtLeast(0f)

    val canScrollHorizontally: Boolean
        get() = maxScrollX > 0f

    val canScrollVertically: Boolean
        get() = maxScrollY > 0f
}