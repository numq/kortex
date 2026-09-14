package io.github.numq.kortex.core.compose.scroll

import androidx.compose.runtime.*
import io.github.numq.kortex.core.compose.layout.EditorLayoutInfo
import io.github.numq.krope.text.TextPosition
import kotlin.math.max

@Stable
class EditorScrollController {
    var horizontalOffset by mutableFloatStateOf(0f)
        private set

    var verticalOffset by mutableFloatStateOf(0f)
        private set

    var layoutInfo by mutableStateOf(EditorLayoutInfo())
        private set

    val viewportWidth: Float get() = layoutInfo.viewportWidth

    val viewportHeight: Float get() = layoutInfo.viewportHeight

    val contentWidth: Float get() = layoutInfo.textContentWidth + layoutInfo.gutterWidth

    val contentHeight: Float get() = layoutInfo.contentHeight

    val gutterWidth: Float get() = layoutInfo.gutterWidth

    val maxScrollX: Float get() = layoutInfo.maxScrollX

    val maxScrollY: Float get() = layoutInfo.maxScrollY

    val canScrollHorizontally: Boolean get() = layoutInfo.canScrollHorizontally

    val canScrollVertically: Boolean get() = layoutInfo.canScrollVertically

    internal fun updateLayoutInfo(info: EditorLayoutInfo) {
        layoutInfo = info

        if (info.viewportWidth > 0f && horizontalOffset > info.maxScrollX) {
            horizontalOffset = info.maxScrollX
        }

        if (info.viewportHeight > 0f && verticalOffset > info.maxScrollY) {
            verticalOffset = info.maxScrollY
        }
    }

    fun scrollTo(horizontal: Float = horizontalOffset, vertical: Float = verticalOffset) {
        val targetX = when {
            layoutInfo.viewportWidth > 0f -> horizontal.coerceIn(0f, layoutInfo.maxScrollX)

            else -> horizontal.coerceAtLeast(0f)
        }

        val targetY = when {
            layoutInfo.viewportHeight > 0f -> vertical.coerceIn(0f, layoutInfo.maxScrollY)

            else -> vertical.coerceAtLeast(0f)
        }

        horizontalOffset = targetX

        verticalOffset = targetY
    }

    fun scrollBy(deltaX: Float = 0f, deltaY: Float = 0f) {
        scrollTo(horizontalOffset + deltaX, verticalOffset + deltaY)
    }

    fun scrollToCaret(
        target: TextPosition,
        charWidth: Float,
        lineHeight: Float,
        gutterWidth: Float = layoutInfo.gutterWidth,
        editorPaddingStart: Float = 0f,
        horizontalMarginChars: Int = 2,
        verticalMarginLines: Int = 1,
    ) {
        if (layoutInfo.viewportHeight <= 0f || layoutInfo.viewportWidth <= 0f) return

        val (line, column) = target

        val absoluteLineTop = line * lineHeight

        val absoluteLineBottom = absoluteLineTop + lineHeight

        val verticalMargin = lineHeight * verticalMarginLines

        val nextY = when {
            absoluteLineTop < verticalOffset + verticalMargin -> {
                if (absoluteLineTop <= verticalMargin) 0f
                else absoluteLineTop - verticalMargin
            }

            absoluteLineBottom > verticalOffset + layoutInfo.viewportHeight - verticalMargin -> {
                val target = absoluteLineBottom - layoutInfo.viewportHeight + verticalMargin

                target.coerceAtMost(layoutInfo.maxScrollY)
            }

            else -> verticalOffset
        }

        val absoluteCaretX = editorPaddingStart + (column * charWidth)

        val horizontalMargin = charWidth * horizontalMarginChars

        val visibleTextWidth = when {
            layoutInfo.textAreaWidth > 0f -> layoutInfo.textAreaWidth

            else -> layoutInfo.textAreaWidth - gutterWidth
        }

        val nextX = when {
            absoluteCaretX < horizontalOffset + horizontalMargin -> max(0f, absoluteCaretX - horizontalMargin)


            visibleTextWidth > 0f && absoluteCaretX > horizontalOffset + visibleTextWidth - horizontalMargin -> absoluteCaretX - visibleTextWidth + horizontalMargin

            else -> horizontalOffset
        }

        scrollTo(horizontal = nextX, vertical = nextY)
    }
}

@Composable
fun rememberEditorScrollController() = remember { EditorScrollController() }