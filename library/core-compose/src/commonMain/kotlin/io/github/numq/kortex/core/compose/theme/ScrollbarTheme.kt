package io.github.numq.kortex.core.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ScrollbarTheme(
    val thumb: Color,
    val track: Color = Color.Transparent,
    val thumbHover: Color = thumb,
)