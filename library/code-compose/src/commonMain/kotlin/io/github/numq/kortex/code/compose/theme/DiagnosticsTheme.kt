package io.github.numq.kortex.code.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class DiagnosticsTheme(
    val error: Color,
    val warning: Color,
    val info: Color,
    val hint: Color,
)