package io.github.numq.kortex.code.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors for diagnostic squiggle lines rendered beneath code issues.
 */
@Immutable
data class DiagnosticsTheme(
    val error: Color,
    val warning: Color,
    val info: Color,
    val hint: Color,
)