package io.github.numq.kortex.code

import io.github.numq.kortex.code.analysis.Analysis
import io.github.numq.kortex.code.analysis.LanguageAnalysisService
import io.github.numq.kortex.code.syntax.Syntax
import io.github.numq.kortex.core.EditorAction
import io.github.numq.kortex.core.EditorCommand
import io.github.numq.kortex.core.EditorEngine
import io.github.numq.kortex.core.text.offsetOf
import io.github.numq.krope.text.TextSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

class CodeEditorEngine(
    private val scope: CoroutineScope,
    val engine: EditorEngine,
    val analysisService: LanguageAnalysisService,
) {
    private val _codeState = MutableStateFlow(CodeEditorState(editor = engine.state.value))

    val state: StateFlow<CodeEditorState> = _codeState.asStateFlow()

    init {
        scope.launch {
            engine.state.collect { coreState ->
                _codeState.update { current ->
                    current.copy(editor = coreState)
                }
            }
        }

        bindAnalysisPipeline()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun bindAnalysisPipeline() {
        analysisService.tokenProvider?.let { provider ->
            engine.buffer.snapshot.debounce(50L.milliseconds).distinctUntilChangedBy(TextSnapshot::revision)
                .mapLatest { snapshot: TextSnapshot ->
                    val tokens = provider.provideTokens(snapshot, null)

                    snapshot.revision to tokens
                }.flowOn(Dispatchers.Default).onEach { (revision, tokens) ->
                    dispatch(
                        CodeEditorAction.External.UpdateSyntax(
                            Syntax(
                                revision = revision,
                                foldingRegions = emptyList(),
                                occurrences = emptyList(),
                                tokensPerLine = tokens
                            )
                        )
                    )
                }.launchIn(scope)
        }

        analysisService.diagnosticsProvider?.let { provider ->
            engine.buffer.snapshot.debounce(300L.milliseconds).distinctUntilChangedBy(TextSnapshot::revision)
                .mapLatest { snapshot: TextSnapshot ->
                    val issues = provider.diagnose(snapshot)

                    snapshot.revision to issues
                }.flowOn(Dispatchers.Default).onEach { (revision, issues) ->
                    dispatch(
                        CodeEditorAction.External.UpdateAnalysis(
                            Analysis(
                                revision = revision, issues = issues
                            )
                        )
                    )
                }.launchIn(scope)
        }
    }

    fun dispatch(action: CodeEditorAction) {
        val (newState, command) = CodeEditorReducer.reduce(_codeState.value, action)

        _codeState.value = newState

        when (command) {
            is CodeEditorCommand.Core -> when (val coreCmd = command.command) {
                is EditorCommand.ApplyText -> engine.applyOperation(coreCmd.operation, coreCmd.canMerge)

                is EditorCommand.Undo -> engine.undo()

                is EditorCommand.Redo -> engine.redo()
            }

            is CodeEditorCommand.ApplyText -> engine.applyOperation(command.operation, command.canMerge)

            null -> Unit
        }
    }

    fun dispatch(action: EditorAction) = dispatch(CodeEditorAction.Core(action))

    fun requestCompletion() {
        val provider = analysisService.completionProvider ?: return

        val snapshot = engine.state.value.snapshot

        val caret = engine.state.value.primarySelection.caret

        scope.launch(Dispatchers.Default) {
            val offset = snapshot.offsetOf(caret)

            val suggestions = provider.provideSuggestions(snapshot, offset)

            dispatch(CodeEditorAction.Completion.Show(suggestions, caret))
        }
    }
}