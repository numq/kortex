package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextRevision

/**
 * Container for diagnostic inspection results bound to a document revision.
 *
 * @property revision The document revision for which diagnostics were evaluated.
 * @property issues List of syntax errors, warnings, linter hints, or inspection notes.
 */
data class Analysis(val revision: TextRevision, val issues: List<CodeIssue>)