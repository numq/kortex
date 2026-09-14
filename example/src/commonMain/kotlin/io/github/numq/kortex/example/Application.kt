package io.github.numq.kortex.example

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import io.github.numq.kortex.code.CodeEditorEngine
import io.github.numq.kortex.code.analysis.LanguageAnalysisService
import io.github.numq.kortex.code.compose.KortexCodeEditor
import io.github.numq.kortex.code.compose.theme.CodeEditorTheme
import io.github.numq.kortex.core.EditorEngine
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.scroll.rememberEditorScrollController
import io.github.numq.kortex.core.compose.scrollbar.rememberHorizontalScrollbarAdapter
import io.github.numq.kortex.core.compose.scrollbar.rememberVerticalScrollbarAdapter
import io.github.numq.krope.core.Encoding
import io.github.numq.krope.text.RopeTextBuffer
import io.github.numq.krope.text.TextLineEnding
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface

private const val SAMPLE_CODE = """package io.github.numq.kortex.example

import io.github.numq.kortex.core.KortexEngine

fun main() {
    val message = "Hello, Kortex High-Performance Skia Editor! Hello, Kortex High-Performance Skia Editor! Hello, Kortex High-Performance Skia Editor!"
    println(message)
    
    // 60+ FPS Hardware-accelerated Skia rendering
}
"""

@Composable
fun Application() {
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

    val font = remember {
        val typeface =
            FontMgr.default.matchFamilyStyle("JetBrains Mono", FontStyle.NORMAL) ?: FontMgr.default.matchFamilyStyle(
                null, FontStyle.NORMAL
            ) ?: Typeface.makeEmpty()

        EditorFont(typeface = typeface, size = 14f, lineSpacing = 1.2f)
    }

    val colorScheme = when {
        isSystemInDarkTheme() -> darkColorScheme()

        else -> lightColorScheme()
    }

    val theme = remember(colorScheme) {
        CodeEditorTheme.fromColorScheme(colorScheme)
    }

    val scrollController = rememberEditorScrollController()

    val verticalScrollbarAdapter = rememberVerticalScrollbarAdapter(scrollController)

    val horizontalScrollbarAdapter = rememberHorizontalScrollbarAdapter(scrollController)

    MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            KortexCodeEditor(
                engine = codeEditorEngine,
                font = font,
                theme = theme,
                scrollController = scrollController,
                modifier = Modifier.fillMaxSize()
            )

            if (scrollController.canScrollVertically) {
                VerticalScrollbar(
                    adapter = verticalScrollbarAdapter, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }

            if (scrollController.canScrollHorizontally) {
                val density = LocalDensity.current

                val gutterPadding = with(density) {
                    scrollController.gutterWidth.toDp()
                }

                HorizontalScrollbar(
                    adapter = horizontalScrollbarAdapter,
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = gutterPadding)
                )
            }
        }
    }
}