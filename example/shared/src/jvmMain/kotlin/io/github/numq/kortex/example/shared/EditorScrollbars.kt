package io.github.numq.kortex.example.shared

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import io.github.numq.kortex.core.compose.scroll.EditorScrollController
import io.github.numq.kortex.core.compose.scrollbar.rememberHorizontalScrollbarAdapter
import io.github.numq.kortex.core.compose.scrollbar.rememberVerticalScrollbarAdapter

@Composable
actual fun EditorScrollbars(scrollController: EditorScrollController, modifier: Modifier) {
    val verticalScrollbarAdapter = rememberVerticalScrollbarAdapter(scrollController)

    val horizontalScrollbarAdapter = rememberHorizontalScrollbarAdapter(scrollController)

    Box(modifier = modifier) {
        if (scrollController.canScrollVertically) {
            VerticalScrollbar(
                adapter = verticalScrollbarAdapter, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }

        if (scrollController.canScrollHorizontally) {
            val density = LocalDensity.current

            val gutterPadding = with(density) {
                scrollController.gutterWidth.toDp()
            }

            HorizontalScrollbar(
                adapter = horizontalScrollbarAdapter,
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = gutterPadding)
            )
        }
    }
}