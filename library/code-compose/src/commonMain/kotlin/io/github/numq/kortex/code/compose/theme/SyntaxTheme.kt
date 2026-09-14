package io.github.numq.kortex.code.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.numq.kortex.code.compose.token.TokenColorResolver
import io.github.numq.kortex.code.token.TokenType

@Immutable
data class SyntaxTheme(
    val defaultText: Color,
    val resolver: TokenColorResolver,
) {
    companion object {
        fun build(
            defaultText: Color,
            fallback: (TokenType) -> Color = { defaultText },
            builder: MutableMap<String, Color>.() -> Unit,
        ): SyntaxTheme {
            val map = buildMap(builder)

            return SyntaxTheme(
                defaultText = defaultText, resolver = { type, fallbackColor ->
                    var current = type.name

                    var matched: Color? = null

                    while (current.isNotEmpty()) {
                        matched = map[current]

                        if (matched != null) break

                        val dotIdx = current.lastIndexOf('.')

                        if (dotIdx == -1) break

                        current = current.substring(0, dotIdx)
                    }

                    matched ?: fallback(type)
                })
        }

        fun defaultDark(colorScheme: ColorScheme) = build(defaultText = colorScheme.onSurface) {
            put(TokenType.KEYWORD.name, colorScheme.primary)

            put(TokenType.TYPE.name, colorScheme.tertiary)

            put(TokenType.FUNCTION.name, colorScheme.secondary)

            put(TokenType.VARIABLE.name, colorScheme.onSurface)

            put(TokenType.STRING.name, Color(0xFF98C379))

            put(TokenType.NUMBER.name, Color(0xFFD19A66))

            put(TokenType.COMMENT.name, colorScheme.onSurfaceVariant.copy(alpha = .5f))

            put(TokenType.OPERATOR.name, colorScheme.primary.copy(alpha = .85f))

            put(TokenType.PUNCTUATION.name, colorScheme.onSurfaceVariant.copy(alpha = .7f))

            put(TokenType.MARKUP_LINK.name, colorScheme.tertiary)
        }

        fun defaultLight(colorScheme: ColorScheme) = build(defaultText = colorScheme.onSurface) {
            put(TokenType.KEYWORD.name, colorScheme.primary)

            put(TokenType.TYPE.name, colorScheme.tertiary)

            put(TokenType.FUNCTION.name, colorScheme.secondary)

            put(TokenType.VARIABLE.name, colorScheme.onSurface)

            put(TokenType.STRING.name, Color(0xFF388E3C))

            put(TokenType.NUMBER.name, Color(0xFFC05621))

            put(TokenType.COMMENT.name, colorScheme.onSurfaceVariant.copy(alpha = .6f))

            put(TokenType.OPERATOR.name, colorScheme.primary)

            put(TokenType.PUNCTUATION.name, colorScheme.onSurfaceVariant.copy(alpha = .8f))

            put(TokenType.MARKUP_LINK.name, colorScheme.primary)
        }
    }
}