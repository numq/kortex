package io.github.numq.kortex.core.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@Immutable
data class EditorTheme(
    val background: Color,
    val text: Color,
    val caret: Color,
    val selection: Color,
    val currentLine: Color,
    val gutter: GutterTheme,
    val scrollbar: ScrollbarTheme,
    val divider: Color = gutter.divider,
) {
    companion object {
        fun fromColorScheme(colorScheme: ColorScheme): EditorTheme = EditorTheme(
            background = colorScheme.surface,
            text = colorScheme.onSurface,
            caret = colorScheme.primary,
            selection = colorScheme.primaryContainer.copy(alpha = .5f),
            currentLine = colorScheme.onSurface.copy(alpha = .05f),
            gutter = GutterTheme(
                background = colorScheme.surfaceContainerLow,
                text = colorScheme.onSurfaceVariant.copy(alpha = .6f),
                currentLineText = colorScheme.primary,
                divider = colorScheme.outlineVariant.copy(alpha = .5f),
            ),
            scrollbar = ScrollbarTheme(
                thumb = colorScheme.onSurfaceVariant.copy(alpha = .3f),
                thumbHover = colorScheme.onSurfaceVariant.copy(alpha = .5f),
            )
        )
    }
}

@Composable
fun rememberEditorTheme(colorScheme: ColorScheme = MaterialTheme.colorScheme) = remember(colorScheme) {
    EditorTheme.fromColorScheme(colorScheme)
}