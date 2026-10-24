package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextRange

/**
 * Hover or quick-doc information for a symbol in the editor.
 *
 * @property content Documentation content (formatted markdown or plain text).
 * @property range Optional text range of the symbol for which this documentation was fetched.
 */
data class CodeDocumentation(val content: String, val range: TextRange? = null)