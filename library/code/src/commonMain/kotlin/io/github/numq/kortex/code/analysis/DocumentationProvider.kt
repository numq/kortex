package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextSnapshot

fun interface DocumentationProvider {
    suspend fun provideDocumentation(snapshot: TextSnapshot, offset: Int): CodeDocumentation?
}