package io.github.numq.kortex.code.compose.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import io.github.numq.kortex.code.CodeEditorState
import io.github.numq.kortex.core.compose.dimensions.EditorDimensions
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.viewport.FoldMap
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun CompletionOverlay(
    completion: CodeEditorState.CompletionState.Visible,
    foldMap: FoldMap,
    font: EditorFont,
    dimensions: EditorDimensions,
    scrollController: EditorScrollController,
    onSelect: (index: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val anchor = completion.anchorPosition
    val visualLine = foldMap.documentToVisualLine(anchor.line)
    val snappedLineHeight = ceil(font.lineHeight)

    val lineTop = (visualLine * snappedLineHeight) - scrollController.verticalOffset
    val lineBottom = lineTop + snappedLineHeight

    val caretX =
        scrollController.gutterWidth + dimensions.editorPaddingStart + (anchor.column * font.charWidth) - scrollController.horizontalOffset

    val listState = rememberLazyListState()

    LaunchedEffect(completion.selectedIndex) {
        if (completion.selectedIndex in completion.suggestions.indices) {
            listState.animateScrollToItem(completion.selectedIndex)
        }
    }

    var parentBoundsInWindow by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
            parentBoundsInWindow = coordinates.positionInWindow()
        }) {
        val positionProvider = remember(
            parentBoundsInWindow,
            caretX,
            lineTop,
            lineBottom,
            scrollController.gutterWidth,
            scrollController.viewportWidth,
            scrollController.viewportHeight,
        ) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    val popupWidth = popupContentSize.width
                    val popupHeight = popupContentSize.height

                    val rawX = parentBoundsInWindow.x + caretX
                    val minX = (parentBoundsInWindow.x + scrollController.gutterWidth).roundToInt()
                    val maxX = (parentBoundsInWindow.x + scrollController.viewportWidth - popupWidth).roundToInt()
                    val x = rawX.roundToInt().coerceIn(minX, maxOf(minX, maxX))

                    val gap = 4
                    val absLineTop = (parentBoundsInWindow.y + lineTop).roundToInt()
                    val absLineBottom = (parentBoundsInWindow.y + lineBottom).roundToInt()

                    val maxWindowBottom = (parentBoundsInWindow.y + scrollController.viewportHeight).roundToInt()
                    val spaceBelow = maxWindowBottom - absLineBottom
                    val spaceAbove = absLineTop - parentBoundsInWindow.y.roundToInt()

                    val y = if (spaceBelow < popupHeight + gap && spaceAbove > spaceBelow) {
                        absLineTop - popupHeight - gap
                    } else {
                        absLineBottom + gap
                    }

                    return IntOffset(x, y)
                }
            }
        }

        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = onDismiss,
        ) {
            Surface(
                modifier = Modifier.widthIn(min = 220.dp, max = 360.dp).heightIn(max = 200.dp),
                shape = MaterialTheme.shapes.small,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                ) {
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
                                .clickable { onSelect(index) }.padding(horizontal = 8.dp, vertical = 6.dp),
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
}