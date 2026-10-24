package io.github.numq.kortex.example.shared

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
actual fun rememberKeyboardBottomInset() = with(LocalDensity.current) {
    WindowInsets.ime.getBottom(this).toFloat()
}