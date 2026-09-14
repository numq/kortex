package io.github.numq.kortex.core.viewport

data class ViewportLine(
    val line: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val text: String,
    val textBaselineY: Float,
)