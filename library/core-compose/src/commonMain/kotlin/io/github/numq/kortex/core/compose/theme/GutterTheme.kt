package io.github.numq.kortex.core.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class GutterTheme(
    val background: Color = Color.Transparent,
    val text: Color,
    val currentLineText: Color,
    val divider: Color,
)