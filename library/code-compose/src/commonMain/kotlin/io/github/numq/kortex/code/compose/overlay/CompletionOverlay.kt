package io.github.numq.kortex.code.compose.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import io.github.numq.kortex.code.CodeEditorState
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.viewport.FoldMap

@Composable
internal fun CompletionOverlay(
    completion: CodeEditorState.CompletionState.Visible,
    foldMap: FoldMap,
    font: EditorFont,
    scrollController: EditorScrollController,
    onSelect: () -> Unit,
    onDismiss: () -> Unit,
) {
    val anchor = completion.anchorPosition

    val visualLine = foldMap.documentToVisualLine(anchor.line)

    val calculatedX =
        scrollController.gutterWidth + (anchor.column * font.charWidth) - scrollController.horizontalOffset

    val calculatedY = (visualLine * font.lineHeight) + font.lineHeight - scrollController.verticalOffset

    val safeX = calculatedX.coerceIn(
        scrollController.gutterWidth, maxOf(scrollController.gutterWidth, scrollController.viewportWidth - 350f)
    )

    val safeY = calculatedY.coerceAtLeast(0f)

    Popup(
        alignment = androidx.compose.ui.Alignment.TopStart,
        offset = IntOffset(safeX.toInt(), safeY.toInt()),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 200.dp, max = 350.dp).heightIn(max = 200.dp),
            shape = MaterialTheme.shapes.small,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                itemsIndexed(completion.suggestions) { index, suggestion ->
                    val isSelected = index == completion.selectedIndex

                    val itemBackground = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer

                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    val itemForeground = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer

                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(itemBackground, shape = MaterialTheme.shapes.extraSmall)
                            .clickable(onClick = onSelect).padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = suggestion.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = itemForeground,
                        )
                        suggestion.detail?.let { detail ->
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.labelSmall,
                                color = itemForeground.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}