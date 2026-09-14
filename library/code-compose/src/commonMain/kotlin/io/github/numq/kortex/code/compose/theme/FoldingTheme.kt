package io.github.numq.kortex.code.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class FoldingTheme(
    val iconColor: Color,
    val expandedIconColor: Color = iconColor.copy(alpha = .6f),
)