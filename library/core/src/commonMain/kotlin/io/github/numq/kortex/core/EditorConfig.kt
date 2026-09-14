package io.github.numq.kortex.core

data class EditorConfig(
    val tabSize: Int = 4,
    val useSoftTabs: Boolean = true,
    val lineSeparator: String = "\n",
) {
    val tabString: String
        get() = when {
            useSoftTabs -> " ".repeat(tabSize)

            else -> "\t"
        }
}