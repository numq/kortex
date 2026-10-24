package io.github.numq.kortex.code.compose.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.numq.kortex.code.token.TokenType

/**
 * Functional interface resolving a Compose [Color] for a given [TokenType].
 */
@Immutable
fun interface TokenColorResolver {
    fun resolveColor(type: TokenType, defaultColor: Color): Color
}