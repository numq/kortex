package io.github.numq.kortex.core.compose.selection

import io.github.numq.kortex.core.compose.layer.Layer
import org.jetbrains.skia.Canvas

data class SelectionLayer(val selectionRegionLayers: List<SelectionRegionLayer> = emptyList()) : Layer {
    override fun render(canvas: Canvas) {
        selectionRegionLayers.forEach { selectionRegionLayer ->
            selectionRegionLayer.render(canvas = canvas)
        }
    }
}