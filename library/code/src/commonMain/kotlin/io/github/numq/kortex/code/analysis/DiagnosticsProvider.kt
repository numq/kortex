package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextSnapshot

fun interface DiagnosticsProvider {
    suspend fun diagnose(snapshot: TextSnapshot): List<CodeIssue>
}