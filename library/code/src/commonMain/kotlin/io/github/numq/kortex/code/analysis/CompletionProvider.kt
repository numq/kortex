package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextSnapshot

fun interface CompletionProvider {
    suspend fun provideSuggestions(snapshot: TextSnapshot, offset: Int): List<CodeSuggestion>
}