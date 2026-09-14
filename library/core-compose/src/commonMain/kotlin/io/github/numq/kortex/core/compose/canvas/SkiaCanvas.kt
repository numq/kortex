package io.github.numq.kortex.core.compose.canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.skia.Canvas

@Composable
expect fun SkiaCanvas(modifier: Modifier = Modifier, onRender: (canvas: Canvas, width: Int, height: Int) -> Unit)