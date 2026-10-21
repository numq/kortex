package io.github.numq.kortex.example.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.numq.kortex.code.CodeEditorEngine
import io.github.numq.kortex.code.analysis.LanguageAnalysisService
import io.github.numq.kortex.code.compose.KortexCodeEditor
import io.github.numq.kortex.code.compose.theme.CodeEditorTheme
import io.github.numq.kortex.core.EditorEngine
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.scroll.rememberEditorScrollController
import io.github.numq.krope.core.Encoding
import io.github.numq.krope.text.RopeTextBuffer
import io.github.numq.krope.text.TextLineEnding
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr

private const val SAMPLE_CODE = """package io.github.numq.kortex.example

import io.github.numq.kortex.core.KortexEngine

fun main() {
    val message = "Hello, Kortex High-Performance Skia Editor! Hello, Kortex High-Performance Skia Editor! Hello, Kortex High-Performance Skia Editor!"
    println(message)
    
    // 60+ FPS Hardware-accelerated Skia rendering
}
"""

@Composable
fun App(fontBytes: ByteArray) {
    val scope = rememberCoroutineScope()

    val buffer = remember {
        RopeTextBuffer(
            initialText = SAMPLE_CODE,
            initialLineEnding = TextLineEnding.CRLF,
            initialEncoding = Encoding.UTF8,
            enablePooling = true
        )
    }

    val editorEngine = remember(buffer) {
        EditorEngine(scope = scope, buffer = buffer)
    }

    val codeEditorEngine = remember(buffer) {
        CodeEditorEngine(
            scope = scope, engine = editorEngine, analysisService = object : LanguageAnalysisService {})
    }

    val density = LocalDensity.current

    val fontSize = with(density) { 14.dp.toPx() }

    val font = remember(fontBytes, fontSize) {
        val data = Data.makeFromBytes(fontBytes)

        val typeface = FontMgr.default.makeFromData(data) ?: error("Skia failed to parse TTF bytes")

        EditorFont(typeface = typeface, size = fontSize, lineSpacing = 1.2f)
    }

    val colorScheme = when {
        isSystemInDarkTheme() -> darkColorScheme()

        else -> lightColorScheme()
    }

    val theme = remember(colorScheme) {
        CodeEditorTheme.fromColorScheme(colorScheme)
    }

    val scrollController = rememberEditorScrollController()

    MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            KortexCodeEditor(
                engine = codeEditorEngine,
                font = font,
                theme = theme,
                scrollController = scrollController,
                modifier = Modifier.fillMaxSize()
            )

            EditorScrollbars(scrollController = scrollController, modifier = Modifier.fillMaxSize())
        }
    }
}