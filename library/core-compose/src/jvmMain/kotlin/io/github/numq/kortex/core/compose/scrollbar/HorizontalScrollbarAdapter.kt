package io.github.numq.kortex.core.compose.scrollbar

import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.numq.kortex.core.compose.scroll.EditorScrollController

fun EditorScrollController.asHorizontalScrollbarAdapter() = object : ScrollbarAdapter {
    override val scrollOffset: Double
        get() = horizontalOffset.toDouble()

    override val viewportSize: Double
        get() = viewportWidth.toDouble()

    override val contentSize: Double
        get() = maxOf(contentWidth, viewportWidth).toDouble()

    override suspend fun scrollTo(scrollOffset: Double) {
        this@asHorizontalScrollbarAdapter.scrollTo(horizontal = scrollOffset.toFloat())
    }
}

@Composable
fun rememberHorizontalScrollbarAdapter(scrollController: EditorScrollController) = remember(scrollController) {
    scrollController.asHorizontalScrollbarAdapter()
}