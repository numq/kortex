package io.github.numq.kortex.example.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.numq.kortex.core.compose.scroll.EditorScrollController

@Composable
expect fun EditorScrollbars(scrollController: EditorScrollController, modifier: Modifier = Modifier)