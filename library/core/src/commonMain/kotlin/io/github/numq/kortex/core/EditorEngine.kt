package io.github.numq.kortex.core

import io.github.numq.kortex.core.history.HistoryEntry
import io.github.numq.kortex.core.history.HistoryManager
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.krope.text.*
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

    private suspend fun executeAtomicOperation(
        operation: TextOperation.Data,
        targetSelections: List<Selection>? = null,
    ): TextEdit.Data? {
        val totalEditData: TextEdit.Data = when (operation) {
            is TextOperation.Data.Single.Insert -> {
                buffer.insert(operation.position, operation.text).getOrNull() ?: return null
            }

            is TextOperation.Data.Single.Delete -> {
                buffer.delete(operation.range).getOrNull() ?: return null
            }

            is TextOperation.Data.Single.Replace -> {
                buffer.replace(operation.range, operation.text).getOrNull() ?: return null
            }

            is TextOperation.Data.Batch -> {
                val sorted = operation.operations.sortedWith { op1, op2 ->
                    getOpStart(op2).compareTo(getOpStart(op1))
                }

                val appliedSingles = ArrayList<TextEdit.Data.Single>(sorted.size)

                for (op in sorted) {
                    val singleResult = when (op) {
                        is TextOperation.Data.Single.Insert -> buffer.insert(op.position, op.text)
                        is TextOperation.Data.Single.Delete -> buffer.delete(op.range)
                        is TextOperation.Data.Single.Replace -> buffer.replace(op.range, op.text)
                    }

                    val singleEdit = singleResult.getOrNull() ?: return null
                    appliedSingles.add(singleEdit)
                }

                TextEdit.Data.Batch(appliedSingles)
            }
        }

        val newSnapshot = buffer.snapshot.value

        _state.update { currentState ->
            val resolvedSelections = targetSelections?.map { selection ->
                selection.coerceIn(newSnapshot)
            } ?: currentState.selections.map { selection ->
                val mappedAnchor = mapPositionThroughDelta(selection.anchor, totalEditData).coerceIn(newSnapshot)
                val mappedCaret = mapPositionThroughDelta(selection.caret, totalEditData).coerceIn(newSnapshot)
                Selection(anchor = mappedAnchor, caret = mappedCaret, stickyColumn = mappedCaret.column)
            }

            currentState.copy(
                snapshot = newSnapshot,
                selections = Selection.normalize(resolvedSelections),
            )
        }

        return totalEditData
    }

    private fun getOpStart(op: TextOperation.Data.Single): TextPosition = when (op) {
        is TextOperation.Data.Single.Insert -> op.position
        is TextOperation.Data.Single.Delete -> op.range.start
        is TextOperation.Data.Single.Replace -> op.range.start
    }

    private fun calculateEndPositionAfterInsert(start: TextPosition, text: String): TextPosition {
        val lines = text.split('\n')
        return if (lines.size == 1) {
            TextPosition(start.line, start.column + text.length)
        } else {
            TextPosition(start.line + lines.size - 1, lines.last().length)
        }
    }

    private fun computeInverseOperation(
        operation: TextOperation.Data,
        snapshotBefore: TextSnapshot,
    ): TextOperation.Data {
        return when (operation) {
            is TextOperation.Data.Single.Insert -> {
                val endPos = calculateEndPositionAfterInsert(operation.position, operation.text)
                TextOperation.Data.Single.Delete(TextRange(operation.position, endPos))
            }

            is TextOperation.Data.Single.Delete -> {
                val deletedText = snapshotBefore.getTextInRange(operation.range)
                TextOperation.Data.Single.Insert(operation.range.start, deletedText)
            }

            is TextOperation.Data.Single.Replace -> {
                val replacedText = snapshotBefore.getTextInRange(operation.range)
                val newEndPos = calculateEndPositionAfterInsert(operation.range.start, operation.text)
                TextOperation.Data.Single.Replace(TextRange(operation.range.start, newEndPos), replacedText)
            }

            is TextOperation.Data.Batch -> {
                val sortedOps = operation.operations.sortedWith { o1, o2 ->
                    getOpStart(o1).compareTo(getOpStart(o2))
                }

                var lineShift = 0
                val inverseSingles = ArrayList<TextOperation.Data.Single>(sortedOps.size)

                for (op in sortedOps) {
                    when (op) {
                        is TextOperation.Data.Single.Insert -> {
                            val shiftedStart = TextPosition(op.position.line + lineShift, op.position.column)
                            val shiftedEnd = calculateEndPositionAfterInsert(shiftedStart, op.text)

                            val linesAdded = op.text.count { it == '\n' }
                            lineShift += linesAdded

                            inverseSingles.add(TextOperation.Data.Single.Delete(TextRange(shiftedStart, shiftedEnd)))
                        }

                        is TextOperation.Data.Single.Delete -> {
                            val shiftedRange = TextRange(
                                TextPosition(op.range.start.line + lineShift, op.range.start.column),
                                TextPosition(op.range.end.line + lineShift, op.range.end.column),
                            )
                            val deletedText = snapshotBefore.getTextInRange(op.range)
                            val linesRemoved = op.range.end.line - op.range.start.line
                            lineShift -= linesRemoved

                            inverseSingles.add(TextOperation.Data.Single.Insert(shiftedRange.start, deletedText))
                        }

                        is TextOperation.Data.Single.Replace -> {
                            val shiftedStart = TextPosition(op.range.start.line + lineShift, op.range.start.column)
                            val newEnd = calculateEndPositionAfterInsert(shiftedStart, op.text)
                            val replacedText = snapshotBefore.getTextInRange(op.range)

                            val linesDiff = op.text.count { it == '\n' } - (op.range.end.line - op.range.start.line)
                            lineShift += linesDiff

                            inverseSingles.add(
                                TextOperation.Data.Single.Replace(
                                    TextRange(shiftedStart, newEnd), replacedText
                                )
                            )
                        }
                    }
                }

                val reversedInverse = inverseSingles.sortedWith { o1, o2 ->
                    getOpStart(o2).compareTo(getOpStart(o1))
                }

                TextOperation.Data.Batch(reversedInverse)
            }
        }
    }

    private suspend fun applyOperationInternal(
        operation: TextOperation.Data,
        canMerge: Boolean,
        targetSelections: List<Selection>? = null,
    ) {
        val stateBefore = _state.value
        executeAtomicOperation(operation, targetSelections = targetSelections) ?: return
        val stateAfter = _state.value

        val inverseOp = computeInverseOperation(operation, stateBefore.snapshot)

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
        executeAtomicOperation(entry.backward, targetSelections = entry.selectionsBefore)
    }

    private suspend fun redoInternal() {
        val entry = history.popRedo() ?: return
        executeAtomicOperation(entry.forward, targetSelections = entry.selectionsAfter)
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