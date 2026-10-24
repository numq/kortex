package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextSnapshot

/**
 * Functional interface for computing code completion suggestions.
 */
fun interface CompletionProvider {
    /**
     * Computes completion items at the specified character offset.
     *
     * @param snapshot The text snapshot of the document.
     * @param offset The 0-based character offset of the caret.
     * @return List of matching suggestions.
     */
    suspend fun provideSuggestions(snapshot: TextSnapshot, offset: Int): List<CodeSuggestion>
}