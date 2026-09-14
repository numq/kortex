package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextRange

data class CodeSuggestion(
    val label: String,
    val kind: Kind,
    val text: String,
    val detail: String? = null,
    val documentation: String? = null,
    val range: TextRange? = null,
) {
    enum class Kind {
        METHOD, FUNCTION, CONSTRUCTOR, FIELD, VARIABLE, CLASS, INTERFACE, MODULE, PROPERTY, UNIT, VALUE, ENUM, KEYWORD, SNIPPET
    }
}