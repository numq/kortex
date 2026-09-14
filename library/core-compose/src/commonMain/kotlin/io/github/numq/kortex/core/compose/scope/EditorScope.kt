package io.github.numq.kortex.core.compose.scope

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Immutable
import io.github.numq.kortex.core.compose.layout.EditorLayoutInfo
import io.github.numq.kortex.core.compose.scroll.EditorScrollController

@LayoutScopeMarker
@Immutable
interface EditorScope {
    val layoutInfo: EditorLayoutInfo

    val scrollController: EditorScrollController
}