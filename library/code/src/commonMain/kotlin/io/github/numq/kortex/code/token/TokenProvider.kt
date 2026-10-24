package io.github.numq.kortex.code.token

import io.github.numq.krope.text.TextRange
import io.github.numq.krope.text.TextSnapshot

/**
 * Functional provider invoked asynchronously to produce syntax tokens for a document snapshot.
 */
fun interface TokenProvider {
    /**
     * Extracts tokens for the specified snapshot.
     *
     * @param snapshot The immutable text snapshot to analyze.
     * @param range Optional sub-range of interest; if `null`, tokens are produced for the entire snapshot.
     * @return Map where the key is the 0-based line index and the value is the list of tokens on that line.
     */
    suspend fun provideTokens(snapshot: TextSnapshot, range: TextRange?): Map<Int, List<Token>>
}