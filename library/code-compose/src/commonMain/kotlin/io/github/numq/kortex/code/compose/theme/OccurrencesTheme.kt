package io.github.numq.kortex.code.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors for highlighting occurrences of the symbol currently under the caret.
 *
 * @property matchBackground Background color for matching symbol occurrences.
 * @property currentMatchBackground Background color for the occurrence directly under the caret.
 */
@Immutable
data class OccurrencesTheme(
    val matchBackground: Color,
    val currentMatchBackground: Color,
)