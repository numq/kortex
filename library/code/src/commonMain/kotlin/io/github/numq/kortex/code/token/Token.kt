package io.github.numq.kortex.code.token

import io.github.numq.krope.text.TextRange

data class Token(val range: TextRange, val type: TokenType)