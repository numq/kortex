package io.github.numq.kortex.code.compose.theme

import androidx.compose.ui.graphics.Color
import io.github.numq.kortex.code.token.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals

class SyntaxThemeTest {
    @Test
    fun `resolves exact token match`() {
        val theme = SyntaxTheme.build(defaultText = Color.Black) {
            put("keyword", Color.Red)
            put("keyword.control", Color.Blue)
        }

        val color = theme.resolver.resolveColor(TokenType.KEYWORD_CONTROL, Color.Black)
        assertEquals(Color.Blue, color)
    }

    @Test
    fun `resolves dot-separated fallback when exact token is not defined`() {
        val theme = SyntaxTheme.build(defaultText = Color.Black) {
            put("function", Color.Cyan)
        }

        val color = theme.resolver.resolveColor(TokenType.FUNCTION_METHOD, Color.Black)
        assertEquals(Color.Cyan, color)
    }

    @Test
    fun `falls back to fallback lambda when no match found`() {
        val fallbackColor = Color.Magenta
        val theme = SyntaxTheme.build(
            defaultText = Color.Black, fallback = { fallbackColor }) {
            put("keyword", Color.Red)
        }

        val color = theme.resolver.resolveColor(TokenType.NUMBER, Color.Transparent)
        assertEquals(fallbackColor, color)
    }

    @Test
    fun `falls back to provided fallbackColor when no fallback lambda is provided`() {
        val defaultTextColor = Color.Black
        val theme = SyntaxTheme.build(defaultText = defaultTextColor) {
            put("keyword", Color.Red)
        }

        val explicitFallback = Color.Magenta
        val color = theme.resolver.resolveColor(TokenType.NUMBER, explicitFallback)
        assertEquals(explicitFallback, color)
    }

    @Test
    fun `falls back to defaultText when fallbackColor is Unspecified and no fallback lambda is provided`() {
        val defaultTextColor = Color.Black
        val theme = SyntaxTheme.build(defaultText = defaultTextColor) {
            put("keyword", Color.Red)
        }

        val color = theme.resolver.resolveColor(TokenType.NUMBER, Color.Unspecified)
        assertEquals(defaultTextColor, color)
    }
}