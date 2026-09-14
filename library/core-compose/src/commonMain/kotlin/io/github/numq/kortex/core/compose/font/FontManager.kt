package io.github.numq.kortex.core.compose.font

import io.github.numq.kortex.core.lifecycle.ManagedResource

interface FontManager : ManagedResource {
    fun loadFontFromBytes(
        bytes: ByteArray, size: Float = DEFAULT_SIZE, lineSpacing: Float = DEFAULT_LINE_SPACING,
    ): Result<EditorFont>

    companion object {
        const val DEFAULT_SIZE = 13f

        const val DEFAULT_LINE_SPACING = 1.2f
    }
}