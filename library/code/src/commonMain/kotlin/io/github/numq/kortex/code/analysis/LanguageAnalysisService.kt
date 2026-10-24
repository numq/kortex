package io.github.numq.kortex.code.analysis

import io.github.numq.kortex.code.token.TokenProvider

/**
 * Facade providing language-specific intelligence (syntax highlighting, diagnostics,
 * code completion, and hover/symbol documentation) to [io.github.numq.kortex.code.CodeEditorEngine].
 *
 * All providers are optional and default to `null` if the language service does not support a capability.
 */
interface LanguageAnalysisService {
    /** Provider for lexical or semantic syntax tokens. */
    val tokenProvider: TokenProvider? get() = null

    /** Provider for background code diagnostics (errors, warnings, hints). */
    val diagnosticsProvider: DiagnosticsProvider? get() = null

    /** Provider for context-aware code completion suggestions. */
    val completionProvider: CompletionProvider? get() = null

    /** Provider for symbol documentation at a given character offset. */
    val documentationProvider: DocumentationProvider? get() = null
}