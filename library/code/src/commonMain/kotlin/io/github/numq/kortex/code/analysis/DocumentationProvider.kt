package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextSnapshot

/**
 * Functional interface for retrieving hover/symbol documentation.
 */
fun interface DocumentationProvider {
    /**
     * Resolves documentation for the symbol at [offset].
     *
     * @param snapshot The document text snapshot.
     * @param offset Character offset of the cursor.
     * @return [CodeDocumentation] if a symbol exists at the given offset, or `null`.
     */
    suspend fun provideDocumentation(snapshot: TextSnapshot, offset: Int): CodeDocumentation?
}