package io.github.numq.kortex.core.compose.font

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.numq.kortex.core.lifecycle.ManagedResource

/**
 * Factory for parsing and creating [EditorFont] instances from raw font bytes.
 */
interface FontManager : ManagedResource {
    fun loadFontFromBytes(
        bytes: ByteArray,
        size: Float = DEFAULT_SIZE,
        lineSpacing: Float = DEFAULT_LINE_SPACING,
    ): Result<EditorFont>

    companion object {
        const val DEFAULT_SIZE = 13f

        const val DEFAULT_LINE_SPACING = 1.2f

        /** Creates a new [FontManager] backed by Skia. */
        fun create(): FontManager = SkiaFontManager()
    }
}

/**
 * Remembers a [FontManager] bound to the Composable lifecycle, closing cached typefaces on disposal.
 */
@Composable
fun rememberFontManager(): FontManager {
    val manager = remember { FontManager.create() }

    DisposableEffect(manager) {
        onDispose {
            manager.close()
        }
    }

    return manager
}