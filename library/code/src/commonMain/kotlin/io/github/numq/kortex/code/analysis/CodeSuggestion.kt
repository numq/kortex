package io.github.numq.kortex.code.analysis

import io.github.numq.krope.text.TextRange

/**
 * An autocomplete item proposed to the user during code completion.
 *
 * @property label The display text shown in the completion dropdown list.
 * @property kind The semantic kind of the suggestion (determines icons or styling).
 * @property text The actual text inserted or replaced in the document when accepted.
 * @property detail Optional secondary text (e.g. return type or package name).
 * @property documentation Optional documentation shown alongside the item.
 * @property range Optional target range to replace; if `null`, [text] is inserted at the anchor caret.
 */
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