package io.github.numq.kortex.core.compose.key

import io.github.numq.kortex.core.EditorAction
import io.github.numq.kortex.core.EditorEngine
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.compose.scroll.EditorScrollController

interface EditorKeyContext {
    val engine: EditorEngine

    val state: EditorState get() = engine.state.value

    val scrollController: EditorScrollController

    val isReadOnly: Boolean

    fun dispatch(action: EditorAction) = engine.dispatch(action)

    fun copy()

    fun cut()

    fun paste()

    fun scrollBy(deltaX: Float = 0f, deltaY: Float = 0f) {
        scrollController.scrollBy(deltaX = deltaX, deltaY = deltaY)
    }

    fun scrollPage(up: Boolean) {
        val sign = when {
            up -> -1f

            else -> 1f
        }

        val amount = scrollController.viewportHeight * .9f

        scrollController.scrollBy(deltaY = amount * sign)
    }
}