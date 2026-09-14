package io.github.numq.kortex.code.token

import io.github.numq.krope.text.TextRange
import io.github.numq.krope.text.TextSnapshot

fun interface TokenProvider {
    suspend fun provideTokens(snapshot: TextSnapshot, range: TextRange?): Map<Int, List<Token>>
}