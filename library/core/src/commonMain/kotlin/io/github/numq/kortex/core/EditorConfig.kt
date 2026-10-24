package io.github.numq.kortex.core

/**
 * Configuration parameters for the text editor behavior.
 *
 * @property tabSize The number of spaces equivalent to a single tab stop.
 * @property useSoftTabs Whether pressing Tab inserts spaces (`true`) or a literal `\t` character (`false`).
 * @property lineSeparator The default line separator used when copying whole lines.
 */
data class EditorConfig(val tabSize: Int = 4, val useSoftTabs: Boolean = true, val lineSeparator: String = "\n") {
    /**
     * The string representation of a single indentation unit based on [useSoftTabs] and [tabSize].
     */
    val tabString: String
        get() = when {
            useSoftTabs -> " ".repeat(tabSize)

            else -> "\t"
        }
}