package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextRange

data class CodeDocumentation(val content: String, val range: TextRange? = null)