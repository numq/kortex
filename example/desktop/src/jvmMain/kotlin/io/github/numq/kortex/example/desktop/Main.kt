package io.github.numq.kortex.example.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.numq.kortex.example.shared.App
import java.io.InputStream

fun main() = application {
    val fontBytes =
        object {}.javaClass.classLoader?.getResourceAsStream("files/JetBrainsMono-Regular.ttf")?.use(InputStream::readBytes)
            ?: error("Font not found in resources")

    Window(onCloseRequest = ::exitApplication, title = "Kortex") {
        App(fontBytes = fontBytes)
    }
}