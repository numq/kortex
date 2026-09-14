package io.github.numq.kortex.code.analysis

import io.github.numq.kortex.code.token.TokenProvider

interface LanguageAnalysisService {
    val tokenProvider: TokenProvider? get() = null

    val diagnosticsProvider: DiagnosticsProvider? get() = null

    val completionProvider: CompletionProvider? get() = null

    val documentationProvider: DocumentationProvider? get() = null
}