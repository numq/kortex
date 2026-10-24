package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextRange

/**
 * Represents a diagnostic problem or notice within the code.
 */
sealed interface CodeIssue {
    /** Document span of the issue. */
    val range: TextRange

    /** User-visible diagnostic message. */
    val message: String

    /** Source of the diagnostic (e.g. "kotlinc", "detekt", "eslint"). */
    val source: String?

    /** Machine-readable error or warning code. */
    val code: String?

    data class Unknown(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null,
    ) : CodeIssue

    data class Error(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null,
    ) : CodeIssue

    data class Warning(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null,
    ) : CodeIssue

    data class Information(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null,
    ) : CodeIssue

    data class Hint(
        override val range: TextRange,
        override val message: String,
        override val source: String? = null,
        override val code: String? = null,
    ) : CodeIssue
}