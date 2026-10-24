package io.github.numq.kortex.core

import io.github.numq.kortex.core.history.HistoryEntry
import io.github.numq.kortex.core.history.HistoryManager
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.krope.text.TextBuffer
import io.github.numq.krope.text.TextEdit
import io.github.numq.krope.text.TextOperation
import io.github.numq.krope.text.TextPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

/**
 * Stateful coordinator that binds a [TextBuffer], [EditorReducer], and [HistoryManager].
 *
 * Serializes all text mutations and state transitions sequentially via an internal mutex,
 * automatically adjusting selections through edit deltas and recording undo/redo history.
 *
 * @param scope The [CoroutineScope] used to observe external buffer changes and execute commands.
 * @property buffer The underlying [TextBuffer] storing the document content.
 * @property history The [HistoryManager] responsible for tracking undo and redo operations.
 */
class EditorEngine(
    private val scope: CoroutineScope,
    val buffer: TextBuffer,
    val history: HistoryManager = HistoryManager(),
) {
    private val operationMutex = Mutex()

    private val _state =
        MutableStateFlow(EditorState(snapshot = buffer.snapshot.value, selections = listOf(Selection.ZERO)))

    /** Reactive stream of the current [EditorState]. */
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        scope.launch {
            buffer.snapshot.drop(1).collect { externalSnapshot ->
                operationMutex.withLock {
                    _state.update { current ->
                        when (current.snapshot.revision) {
                            externalSnapshot.revision -> current

                            else -> current.copy(
                                snapshot = externalSnapshot,
                                selections = Selection.normalize(
                                    current.selections.map { selection ->
                                        selection.coerceIn(externalSnapshot)
                                    }),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun mapPositionThroughDelta(pos: TextPosition, data: TextEdit.Data) = when (data) {
        is TextEdit.Data.Single -> mapSingleDelta(pos, data)

        is TextEdit.Data.Batch -> data.singles.fold(pos) { acc, single ->
            mapSingleDelta(acc, single)
        }
    }

    private fun mapSingleDelta(position: TextPosition, delta: TextEdit.Data.Single): TextPosition {
        val start = delta.startPosition
        val oldEnd = delta.oldEndPosition
        val newEnd = delta.newEndPosition

        return when {
            position < start -> position
            position in start..oldEnd -> newEnd
            else -> {
                val lineDelta = newEnd.line - oldEnd.line
                when (position.line) {
                    oldEnd.line -> {
                        val colDelta = newEnd.column - oldEnd.column
                        TextPosition(line = position.line + lineDelta, column = position.column + colDelta)
                    }

                    else -> TextPosition(line = position.line + lineDelta, column = position.column)
                }
            }
        }
    }

    private suspend fun executeOperation(
        operation: TextOperation.Data,
        targetSelections: List<Selection>? = null,
    ): TextEdit.Data? {
        val safeOperation = when (operation) {
            is TextOperation.Data.Batch -> {
                val sortedOperations = operation.operations.sortedByDescending { op ->
                    when (op) {
                        is TextOperation.Data.Single.Insert -> op.position
                        is TextOperation.Data.Single.Delete -> op.range.start
                        is TextOperation.Data.Single.Replace -> op.range.start
                    }
                }
                TextOperation.Data.Batch(sortedOperations)
            }

            else -> operation
        }

        val editDataResult = when (safeOperation) {
            is TextOperation.Data.Single.Insert -> buffer.insert(safeOperation.position, safeOperation.text)
            is TextOperation.Data.Single.Delete -> buffer.delete(safeOperation.range)
            is TextOperation.Data.Single.Replace -> buffer.replace(safeOperation.range, safeOperation.text)
            is TextOperation.Data.Batch -> buffer.withBatch { batchBuffer ->
                for (op in safeOperation.operations) {
                    when (op) {
                        is TextOperation.Data.Single.Insert -> batchBuffer.insert(op.position, op.text)
                        is TextOperation.Data.Single.Delete -> batchBuffer.delete(op.range)
                        is TextOperation.Data.Single.Replace -> batchBuffer.replace(op.range, op.text)
                    }
                }
            }
        }

        val editData = editDataResult.getOrNull() ?: return null
        val newSnapshot = buffer.snapshot.value

        _state.update { currentState ->
            val resolvedSelections = targetSelections?.map { selection ->
                selection.coerceIn(newSnapshot)
            } ?: currentState.selections.map { selection ->
                val mappedAnchor = mapPositionThroughDelta(selection.anchor, editData).coerceIn(newSnapshot)
                val mappedCaret = mapPositionThroughDelta(selection.caret, editData).coerceIn(newSnapshot)
                Selection(anchor = mappedAnchor, caret = mappedCaret, stickyColumn = mappedCaret.column)
            }

            currentState.copy(
                snapshot = newSnapshot,
                selections = Selection.normalize(resolvedSelections),
            )
        }

        return editData
    }

    private suspend fun applyOperationInternal(
        operation: TextOperation.Data,
        canMerge: Boolean,
        targetSelections: List<Selection>? = null,
    ) {
        val stateBefore = _state.value
        val editData = executeOperation(operation, targetSelections = targetSelections) ?: return
        val stateAfter = _state.value

        val inverseData = editData.invert()

        val inverseOp = when (val rawInverseOperation = inverseData.toOperationData(stateAfter.snapshot)) {
            is TextOperation.Data.Batch -> {
                val sortedSingles = rawInverseOperation.operations.sortedByDescending { op ->
                    when (op) {
                        is TextOperation.Data.Single.Insert -> op.position
                        is TextOperation.Data.Single.Delete -> op.range.start
                        is TextOperation.Data.Single.Replace -> op.range.start
                    }
                }
                TextOperation.Data.Batch(sortedSingles)
            }

            else -> rawInverseOperation
        }

        history.record(
            HistoryEntry(
                forward = operation,
                backward = inverseOp,
                selectionsBefore = stateBefore.selections,
                selectionsAfter = stateAfter.selections,
                timestamp = TimeSource.Monotonic.markNow(),
                canMerge = canMerge,
            )
        )
    }

    private suspend fun undoInternal() {
        val entry = history.popUndo() ?: return
        executeOperation(entry.backward, targetSelections = entry.selectionsBefore)
    }

    private suspend fun redoInternal() {
        val entry = history.popRedo() ?: return
        executeOperation(entry.forward, targetSelections = entry.selectionsAfter)
    }

    /**
     * Dispatches an [EditorAction] to update the editor state and execute any resulting [EditorCommand].
     */
    fun dispatch(action: EditorAction) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            operationMutex.withLock {
                val (newState, command) = EditorReducer.reduce(_state.value, action)
                _state.value = newState

                when (command) {
                    is EditorCommand.ApplyText -> applyOperationInternal(
                        operation = command.operation,
                        canMerge = command.canMerge,
                        targetSelections = command.targetSelections,
                    )

                    is EditorCommand.Undo -> undoInternal()
                    is EditorCommand.Redo -> redoInternal()
                    null -> Unit
                }
            }
        }
    }

    /**
     * Directly applies a low-level [TextOperation.Data] to the buffer and records it in [history].
     *
     * @param operation The text operation (single or batch) to execute.
     * @param canMerge Whether this operation can be merged with adjacent typing/deletion history entries.
     * @param targetSelections Optional explicit selections to set after the operation completes.
     */
    fun applyOperation(
        operation: TextOperation.Data,
        canMerge: Boolean = true,
        targetSelections: List<Selection>? = null,
    ) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            operationMutex.withLock {
                applyOperationInternal(operation, canMerge, targetSelections)
            }
        }
    }

    /** Reverts the most recent undoable operation in [history], if available. */
    fun undo() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            operationMutex.withLock {
                undoInternal()
            }
        }
    }

    /** Re-applies the most recently undone operation in [history], if available. */
    fun redo() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            operationMutex.withLock {
                redoInternal()
            }
        }
    }
}