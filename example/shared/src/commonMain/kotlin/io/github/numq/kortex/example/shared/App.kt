package io.github.numq.kortex.example.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.numq.kortex.code.CodeEditorAction
import io.github.numq.kortex.code.CodeEditorEngine
import io.github.numq.kortex.code.analysis.*
import io.github.numq.kortex.code.compose.KortexCodeEditor
import io.github.numq.kortex.code.compose.theme.CodeEditorTheme
import io.github.numq.kortex.code.syntax.Syntax
import io.github.numq.kortex.code.token.TokenProvider
import io.github.numq.kortex.core.EditorEngine
import io.github.numq.kortex.core.compose.font.EditorFont
import io.github.numq.kortex.core.compose.scroll.rememberEditorScrollController
import io.github.numq.krope.core.Encoding
import io.github.numq.krope.text.RopeTextBuffer
import io.github.numq.krope.text.TextLineEnding
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.impl.use

private const val SAMPLE_CODE = """package io.github.numq.kortex.example

import io.github.numq.kortex.core.EditorEngine

// 60+ FPS Hardware-accelerated Skia rendering
class DemoService(val engine: EditorEngine) {
    fun render() {
        val message = "Hello, Kortex High-Performance Editor!"
        println(message)
    }
}

fun main() {
    val service = DemoService(engine = EditorEngine())
    service.render()
}
"""

@Composable
fun App(fontBytes: ByteArray) {
    val scope = rememberCoroutineScope()

    val buffer = remember {
        RopeTextBuffer(
            initialText = SAMPLE_CODE,
            initialLineEnding = TextLineEnding.LF,
            initialEncoding = Encoding.UTF8,
            enablePooling = true,
        )
    }

    val editorEngine = remember(buffer) {
        EditorEngine(scope = scope, buffer = buffer)
    }

    val tokenProvider = remember { KotlinTokenProvider() }

    val codeEditorEngine = remember(editorEngine) {
        CodeEditorEngine(
            scope = scope,
            engine = editorEngine,
            analysisService = object : LanguageAnalysisService {
                override val tokenProvider: TokenProvider = tokenProvider

                override val completionProvider: CompletionProvider = CompletionProvider { snapshot, offset ->
                    listOf(
                        CodeSuggestion(
                            label = "println(message)",
                            kind = CodeSuggestion.Kind.FUNCTION,
                            text = "println()",
                            detail = "Unit",
                            documentation = "Prints the given message to the standard output stream."
                        ), CodeSuggestion(
                            label = "message", kind = CodeSuggestion.Kind.VARIABLE, text = "message", detail = "String"
                        ), CodeSuggestion(
                            label = "render()", kind = CodeSuggestion.Kind.METHOD, text = "render()", detail = "Unit"
                        ), CodeSuggestion(
                            label = "DemoService",
                            kind = CodeSuggestion.Kind.CLASS,
                            text = "DemoService",
                            detail = "class"
                        ), CodeSuggestion(
                            label = "EditorEngine",
                            kind = CodeSuggestion.Kind.CLASS,
                            text = "EditorEngine",
                            detail = "class"
                        )
                    )
                }

                override val documentationProvider: DocumentationProvider = DocumentationProvider { snapshot, offset ->
                    CodeDocumentation("Kortex: High-performance Skia Text Editor Component for Compose Multiplatform.")
                }
            },
        )
    }

    val initialFolds = remember(buffer) {
        tokenProvider.calculateFoldingRegions(buffer.snapshot.value)
    }

    LaunchedEffect(codeEditorEngine) {
        val snapshot = buffer.snapshot.value
        val tokens = tokenProvider.provideTokens(snapshot, null)
        codeEditorEngine.dispatch(
            CodeEditorAction.External.UpdateSyntax(
                Syntax(
                    revision = snapshot.revision,
                    foldingRegions = initialFolds,
                    occurrences = emptyList(),
                    tokensPerLine = tokens
                )
            )
        )
    }

    LaunchedEffect(codeEditorEngine, tokenProvider) {
        editorEngine.buffer.snapshot.distinctUntilChangedBy { it.revision }.onEach { snapshot ->
            val folds = tokenProvider.calculateFoldingRegions(snapshot)
            val currentSyntax = codeEditorEngine.state.value.syntax
            if (currentSyntax != null) {
                codeEditorEngine.dispatch(
                    CodeEditorAction.External.UpdateSyntax(
                        currentSyntax.copy(
                            revision = snapshot.revision, foldingRegions = folds
                        )
                    )
                )
            }
        }.launchIn(this)
    }

    val density = LocalDensity.current
    val fontSize = with(density) { 14.dp.toPx() }

    val font = remember(fontBytes, fontSize) {
        val typeface = Data.makeFromBytes(fontBytes).use { data ->
            FontMgr.default.makeFromData(data) ?: error("Skia failed to parse TTF bytes")
        }

        EditorFont(typeface = typeface, size = fontSize, lineSpacing = 1.25f)
    }

    DisposableEffect(font) {
        onDispose {
            font.close()
        }
    }

    val colorScheme = when {
        isSystemInDarkTheme() -> darkColorScheme()
        else -> lightColorScheme()
    }

    val theme = remember(colorScheme) {
        CodeEditorTheme.fromColorScheme(colorScheme)
    }

    val scrollController = rememberEditorScrollController()
    val keyboardInset = rememberKeyboardBottomInset()

    MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = Modifier.fillMaxSize().background(theme.editor.background).systemBarsPadding()) {
            KortexCodeEditor(
                engine = codeEditorEngine,
                font = font,
                theme = theme,
                scrollController = scrollController,
                bottomInset = keyboardInset,
                columnRuler = 80,
                modifier = Modifier.fillMaxSize(),
            )

            EditorScrollbars(scrollController = scrollController, modifier = Modifier.fillMaxSize())
        }
    }
}