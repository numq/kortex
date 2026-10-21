package io.github.numq.kortex.example.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.numq.kortex.example.shared.App
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        val fontBytes = assets.open("JetBrainsMono-Regular.ttf").use(InputStream::readBytes)

        setContent {
            App(fontBytes = fontBytes)
        }
    }
}