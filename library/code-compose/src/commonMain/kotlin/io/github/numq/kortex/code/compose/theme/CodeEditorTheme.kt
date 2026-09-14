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

@Composable
fun rememberCodeEditorTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme, isDark: Boolean = isSystemInDarkTheme()
) = remember(colorScheme, isDark) {
    CodeEditorTheme.fromColorScheme(colorScheme, isDark)
}