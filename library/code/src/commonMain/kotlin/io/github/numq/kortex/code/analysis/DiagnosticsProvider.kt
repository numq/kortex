package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextSnapshot

/**
 * Functional interface for running document diagnostics in the background.
 */
fun interface DiagnosticsProvider {
    /**
     * Inspects the document and returns identified issues.
     *
     * @param snapshot The current text snapshot.
     * @return List of detected issues.
     */
    suspend fun diagnose(snapshot: TextSnapshot): List<CodeIssue>
}