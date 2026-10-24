package io.github.numq.kortex.code.token

import io.github.numq.krope.text.TextRange

/**
 * Represents a lexical or semantic token within the source text.
 *
 * @property range The text coordinate range spanned by this token.
 * @property type The syntactic classification of the token.
 */
data class Token(val range: TextRange, val type: TokenType)