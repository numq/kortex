package io.github.numq.kortex.code.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.numq.kortex.core.compose.theme.EditorTheme

/**
 * Complete visual theme for [io.github.numq.kortex.code.compose.KortexCodeEditor].
 *
 * @property editor Base theme for the editor chrome, gutter, and caret.
 * @property syntax Theme for syntax highlighting.
 * @property diagnostics Colors for diagnostic issue underlines.
 * @property occurrences Colors for symbol occurrence highlights.
 * @property folding Colors for folding triangles.
 * @property ruler Color of the vertical column guide.
 */
@Immutable
data class CodeEditorTheme(
    val editor: EditorTheme,
    val syntax: SyntaxTheme,
    val diagnostics: DiagnosticsTheme,
    val occurrences: OccurrencesTheme,
    val folding: FoldingTheme,
    val ruler: Color = editor.gutter.divider.copy(alpha = .3f),
) {
    companion object {
        /** Creates a [CodeEditorTheme] adapted to the provided Material 3 [colorScheme]. */
        fun fromColorScheme(
            colorScheme: ColorScheme, isDark: Boolean = colorScheme.surface.luminance() < .5f
        ): CodeEditorTheme {
            val base = EditorTheme.fromColorScheme(colorScheme)

            return CodeEditorTheme(
                editor = base, syntax = when {
                    isDark -> SyntaxTheme.defaultDark(colorScheme)

                    else -> SyntaxTheme.defaultLight(colorScheme)
                }, diagnostics = DiagnosticsTheme(
                    error = colorScheme.error,
                    warning = Color(0xFFFFA000),
                    info = colorScheme.tertiary,
                    hint = colorScheme.outline,
                ), occurrences = OccurrencesTheme(
                    matchBackground = colorScheme.primary.copy(alpha = .15f),
                    currentMatchBackground = colorScheme.primary.copy(alpha = .3f),
                ), folding = FoldingTheme(
                    iconColor = colorScheme.onSurfaceVariant.copy(alpha = .7f)
                )
            )
        }
    }
}

/** Remembers a [CodeEditorTheme] adapting dynamically to dark/light theme state. */
@Composable
fun rememberCodeEditorTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme, isDark: Boolean = isSystemInDarkTheme()
) = remember(colorScheme, isDark) {
    CodeEditorTheme.fromColorScheme(colorScheme, isDark)
}