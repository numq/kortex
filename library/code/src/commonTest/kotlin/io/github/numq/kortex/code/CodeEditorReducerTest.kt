package io.github.numq.kortex.code

import io.github.numq.kortex.code.analysis.CodeSuggestion
import io.github.numq.kortex.core.EditorConfig
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.krope.core.Encoding
import io.github.numq.krope.text.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeEditorReducerTest {
    private fun createStubState(): CodeEditorState {
        val snapshot = object : TextSnapshot {
            override fun getBytePosition(position: TextPosition): Int? {
                TODO("Not yet implemented")
            }

            override fun getLineLength(line: Int): Int {
                TODO("Not yet implemented")
            }

            override fun getLineText(line: Int): String {
                TODO("Not yet implemented")
            }

            override fun getTextInRange(range: TextRange): String {
                TODO("Not yet implemented")
            }

            override fun getTextPosition(bytePosition: Int): TextPosition? {
                TODO("Not yet implemented")
            }

            override fun isValidPosition(position: TextPosition): Boolean {
                TODO("Not yet implemented")
            }

            override val encoding: Encoding
                get() = TODO("Not yet implemented")
            override val lastPosition: TextPosition
                get() = TODO("Not yet implemented")
            override val lineEnding: TextLineEnding
                get() = TODO("Not yet implemented")
            override val lines: Int
                get() = TODO("Not yet implemented")
            override val maxLineLength: Int
                get() = TODO("Not yet implemented")
            override val revision: TextRevision
                get() = TODO("Not yet implemented")
            override val text: String
                get() = TODO("Not yet implemented")
        }
        val editor = EditorState(snapshot = snapshot, selections = listOf(Selection.ZERO), config = EditorConfig())
        return CodeEditorState(editor = editor)
    }

    @Test
    fun `shows completion suggestions and navigates with SelectNext and SelectPrevious`() {
        val state = createStubState()
        val suggestions = listOf(
            CodeSuggestion("testA", CodeSuggestion.Kind.FUNCTION, "testA()"),
            CodeSuggestion("testB", CodeSuggestion.Kind.FUNCTION, "testB()")
        )

        val showResult = CodeEditorReducer.reduce(
            state,
            CodeEditorAction.Completion.Show(suggestions, TextPosition(0, 0))
        )
        val visibleState = showResult.state.completion as? CodeEditorState.CompletionState.Visible
        assertNotNull(visibleState)
        assertEquals(0, visibleState.selectedIndex)

        val nextResult = CodeEditorReducer.reduce(showResult.state, CodeEditorAction.Completion.SelectNext)
        val nextVisible = nextResult.state.completion as CodeEditorState.CompletionState.Visible
        assertEquals(1, nextVisible.selectedIndex)

        val prevResult = CodeEditorReducer.reduce(nextResult.state, CodeEditorAction.Completion.SelectPrevious)
        val prevVisible = prevResult.state.completion as CodeEditorState.CompletionState.Visible
        assertEquals(0, prevVisible.selectedIndex)
    }

    @Test
    fun `moving caret dismisses active completion overlay`() {
        val state = createStubState()
        val suggestions = listOf(CodeSuggestion("item", CodeSuggestion.Kind.VARIABLE, "item"))
        val showResult = CodeEditorReducer.reduce(
            state,
            CodeEditorAction.Completion.Show(suggestions, TextPosition(0, 0))
        )

        assertTrue(showResult.state.completion is CodeEditorState.CompletionState.Visible)

        val moveResult = CodeEditorReducer.reduce(
            showResult.state,
            CodeEditorAction.Core(CaretAction.Move(TextPosition(0, 0)))
        )

        assertEquals(CodeEditorState.CompletionState.Hidden, moveResult.state.completion)
    }
}