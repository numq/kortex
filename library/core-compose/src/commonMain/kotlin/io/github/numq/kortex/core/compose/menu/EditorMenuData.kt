package io.github.numq.kortex.core.compose.menu

import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange

data class EditorMenuData(
    val x: Float,
    val y: Float,
    val position: TextPosition?,
    val selectedRange: TextRange,
    val selectedText: String,
    val isReadOnly: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val cut: () -> Unit,
    val copy: () -> Unit,
    val paste: () -> Unit,
    val undo: () -> Unit,
    val redo: () -> Unit,
    val selectAll: () -> Unit,
    val dismiss: () -> Unit,
) {
    val canCut: Boolean get() = !isReadOnly && selectedRange.isNotEmpty

    val canCopy: Boolean get() = selectedRange.isNotEmpty

    val canPaste: Boolean get() = !isReadOnly
}