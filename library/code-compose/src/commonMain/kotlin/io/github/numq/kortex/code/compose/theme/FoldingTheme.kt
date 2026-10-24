package io.github.numq.kortex.code.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Visual styling configuration for code folding markers in the gutter.
 *
 * @property iconColor Color used for collapsed folding indicators.
 */
@Immutable
data class FoldingTheme(val iconColor: Color)