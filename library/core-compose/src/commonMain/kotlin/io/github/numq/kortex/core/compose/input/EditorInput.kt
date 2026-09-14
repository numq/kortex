package io.github.numq.kortex.core.compose.input

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed

val KeyEvent.isPrimaryModifierPressed: Boolean
    get() = this.isCtrlPressed || this.isMetaPressed