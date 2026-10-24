package io.github.numq.kortex.code

import io.github.numq.kortex.code.analysis.Analysis
import io.github.numq.kortex.code.analysis.LanguageAnalysisService
import io.github.numq.kortex.code.syntax.Syntax
import io.github.numq.kortex.core.EditorAction
import io.github.numq.kortex.core.EditorCommand
import io.github.numq.kortex.core.EditorEngine
import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.selection.SelectionAction
import io.github.numq.kortex.core.text.offsetOf
import io.github.numq.kortex.core.viewport.ViewportAction
import io.github.numq.krope.text.TextSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * High-level engine for a code editor that wraps a core [EditorEngine] and integrates
 * asynchronous syntax tokenization, diagnostics, documentation, and code completion via [LanguageAnalysisService].
 *
 * @param scope The [CoroutineScope] managing background analysis pipelines and state synchronization.
 * @property engine The underlying core [EditorEngine].
 * @property analysisService The language analysis providers bound to this editor instance.
 */
class CodeEditorEngine(
    private val scope: CoroutineScope,
    val engine: EditorEngine,
    val analysisService: LanguageAnalysisService,
) {
    private val _codeState = MutableStateFlow(CodeEditorState(editor = engine.state.value))

    private var completionJob: Job? = null
    private var documentationJob: Job? = null

    /** Reactive stream of the current [CodeEditorState]. */
    val state: StateFlow<CodeEditorState> = _codeState.asStateFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            engine.state.collect { coreState ->
                _codeState.update { current ->
                    val caretChanged = current.editor.caret != coreState.caret
                    val updatedCompletion = when {
                        caretChanged -> CodeEditorState.CompletionState.Hidden
                        else -> current.completion
                    }
                    val updatedDocumentation = when {
                        caretChanged -> null
                        else -> current.documentation
                    }

                    current.copy(
                        editor = coreState,
                        completion = updatedCompletion,
                        documentation = updatedDocumentation,
                    )
                }
            }
        }

        bindAnalysisPipeline()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun bindAnalysisPipeline() {
        analysisService.tokenProvider?.let { provider ->
            engine.buffer.snapshot.distinctUntilChangedBy(TextSnapshot::revision).mapLatest { snapshot: TextSnapshot ->
                val tokens = provider.provideTokens(snapshot, null)
                snapshot.revision to tokens
            }.flowOn(Dispatchers.Default).onEach { (revision, tokens) ->
                if (engine.buffer.snapshot.value.revision == revision) {
                    val currentSyntax = _codeState.value.syntax
                    dispatch(
                        CodeEditorAction.External.UpdateSyntax(
                            Syntax(
                                revision = revision,
                                foldingRegions = currentSyntax?.foldingRegions.orEmpty(),
                                occurrences = currentSyntax?.occurrences.orEmpty(),
                                tokensPerLine = tokens,
                            )
                        )
                    )
                }
            }.launchIn(scope)
        }

        analysisService.diagnosticsProvider?.let { provider ->
            engine.buffer.snapshot.debounce(300L.milliseconds).distinctUntilChangedBy(TextSnapshot::revision)
                .mapLatest { snapshot: TextSnapshot ->
                    val issues = provider.diagnose(snapshot)

                    snapshot.revision to issues
                }.flowOn(Dispatchers.Default).onEach { (revision, issues) ->
                    if (engine.buffer.snapshot.value.revision == revision) {
                        dispatch(
                            CodeEditorAction.External.UpdateAnalysis(
                                Analysis(
                                    revision = revision,
                                    issues = issues,
                                )
                            )
                        )
                    }
                }.launchIn(scope)
        }

        analysisService.documentationProvider?.let { provider ->
            engine.state.map { editorState -> editorState.snapshot to editorState.primarySelection.caret }
                .distinctUntilChanged { (oldSnap, oldCaret), (newSnap, newCaret) ->
                    oldSnap.revision == newSnap.revision && oldCaret == newCaret
                }.debounce(250L.milliseconds).mapLatest { (snapshot, caret) ->
                    val offset = snapshot.offsetOf(caret)
                    val doc = provider.provideDocumentation(snapshot, offset)
                    Triple(snapshot.revision, caret, doc)
                }.flowOn(Dispatchers.Default).onEach { (revision, caret, doc) ->
                    val current = engine.state.value
                    if (current.snapshot.revision == revision && current.primarySelection.caret == caret) {
                        dispatch(CodeEditorAction.External.UpdateDocumentation(doc))
                    }
                }.launchIn(scope)
        }
    }

    /** Dispatches a [CodeEditorAction] to the code editor reducer or delegates core actions to [engine]. */
    fun dispatch(action: CodeEditorAction) {
        if (action is CodeEditorAction.Core) {
            if (action.action is CaretAction.Move || action.action is SelectionAction.Clear) {
                _codeState.update { current ->
                    current.copy(
                        completion = CodeEditorState.CompletionState.Hidden,
                        documentation = null,
                    )
                }
            }
            engine.dispatch(action.action)
            return
        }

        var emittedCommand: CodeEditorCommand? = null
        var syncedRanges: List<IntRange>? = null
        var syncedCollapsedLines: Set<Int>? = null

        _codeState.update { current ->
            val (newState, command) = CodeEditorReducer.reduce(current, action)
            emittedCommand = command

            if (action is CodeEditorAction.External.UpdateSyntax) {
                if (newState.editor.collapsedRanges != current.editor.collapsedRanges) {
                    syncedRanges = newState.editor.collapsedRanges
                }
                if (newState.editor.collapsedLines != current.editor.collapsedLines) {
                    syncedCollapsedLines = newState.editor.collapsedLines
                }
            }

            newState
        }

        syncedRanges?.let { ranges ->
            engine.dispatch(ViewportAction.UpdateFoldingRegions(ranges))
        }
        syncedCollapsedLines?.let { lines ->
            engine.dispatch(ViewportAction.UpdateCollapsedLines(lines))
        }

        when (val command = emittedCommand) {
            is CodeEditorCommand.Core -> when (val coreCmd = command.command) {
                is EditorCommand.ApplyText -> engine.applyOperation(
                    operation = coreCmd.operation,
                    canMerge = coreCmd.canMerge,
                    targetSelections = coreCmd.targetSelections,
                )

                is EditorCommand.Undo -> engine.undo()

                is EditorCommand.Redo -> engine.redo()
            }

            is CodeEditorCommand.ApplyText -> engine.applyOperation(command.operation, command.canMerge)

            null -> Unit
        }
    }

    /** Convenience overload to dispatch a core [EditorAction] directly. */
    fun dispatch(action: EditorAction) = dispatch(CodeEditorAction.Core(action))

    /**
     * Asynchronously queries [LanguageAnalysisService.completionProvider] at the current primary caret
     * position and displays the completion popup if suggestions are returned.
     */
    fun requestCompletion() {
        val provider = analysisService.completionProvider ?: return

        val snapshot = engine.state.value.snapshot
        val caret = engine.state.value.primarySelection.caret

        completionJob?.cancel()
        completionJob = scope.launch(Dispatchers.Default) {
            val offset = snapshot.offsetOf(caret)
            val suggestions = provider.provideSuggestions(snapshot, offset)

            val currentEditorState = engine.state.value
            if (isActive && currentEditorState.snapshot.revision == snapshot.revision && currentEditorState.primarySelection.caret == caret) {
                dispatch(CodeEditorAction.Completion.Show(suggestions, caret))
            }
        }
    }

    /**
     * Asynchronously queries [LanguageAnalysisService.documentationProvider] at the current primary caret
     * position and updates [CodeEditorState.documentation].
     */
    fun requestDocumentation() {
        val provider = analysisService.documentationProvider ?: return

        val snapshot = engine.state.value.snapshot
        val caret = engine.state.value.primarySelection.caret

        documentationJob?.cancel()
        documentationJob = scope.launch(Dispatchers.Default) {
            val offset = snapshot.offsetOf(caret)
            val doc = provider.provideDocumentation(snapshot, offset)

            val currentEditorState = engine.state.value
            if (isActive && currentEditorState.snapshot.revision == snapshot.revision && currentEditorState.primarySelection.caret == caret) {
                dispatch(CodeEditorAction.External.UpdateDocumentation(doc))
            }
        }
    }
}