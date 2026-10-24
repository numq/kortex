package io.github.numq.kortex.code

import io.github.numq.kortex.code.analysis.CodeSuggestion
import io.github.numq.kortex.code.syntax.FoldingRegion
import io.github.numq.kortex.code.syntax.Syntax
import io.github.numq.kortex.core.EditorConfig
import io.github.numq.kortex.core.EditorState
import io.github.numq.kortex.core.caret.CaretAction
import io.github.numq.kortex.core.selection.Selection
import io.github.numq.krope.core.Encoding
import io.github.numq.krope.text.*
import kotlin.test.*

class CodeEditorReducerTest {
    private fun createState(
        text: String = "",
        selections: List<Selection> = listOf(Selection.ZERO),
    ): CodeEditorState {
        val lines = text.split("\n")
        val snapshot = object : TextSnapshot {
            override val text: String = text
            override val lines: Int = lines.size
            override val maxLineLength: Int = lines.maxOfOrNull { it.length } ?: 0
            override val lastPosition: TextPosition = TextPosition(lines.lastIndex, lines.last().length)
            override val lineEnding: TextLineEnding = TextLineEnding.LF
            override val revision: TextRevision = TextRevision(1L)
            override val encoding: Encoding = Encoding.UTF8
            override fun getLineText(line: Int): String = lines.getOrElse(line) { "" }
            override fun getLineLength(line: Int): Int = getLineText(line).length
            override fun getTextInRange(range: TextRange): String = ""
            override fun isValidPosition(position: TextPosition): Boolean = true
            override fun getBytePosition(position: TextPosition): Int? = null
            override fun getTextPosition(bytePosition: Int): TextPosition? = null
        }
        return CodeEditorState(
            editor = EditorState(
                snapshot = snapshot, selections = selections, config = EditorConfig()
            )
        )
    }

    @Test
    fun `shows completion suggestions and navigates with SelectNext and SelectPrevious`() {
        val state = createState()
        val suggestions = listOf(
            CodeSuggestion("testA", CodeSuggestion.Kind.FUNCTION, "testA()"),
            CodeSuggestion("testB", CodeSuggestion.Kind.FUNCTION, "testB()")
        )

        val showResult = CodeEditorReducer.reduce(
            state, CodeEditorAction.Completion.Show(suggestions, TextPosition(0, 0))
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
    fun `apply completion generates Replace command when suggestion has range`() {
        val state = createState("pri")
        val replaceRange = TextRange(TextPosition(0, 0), TextPosition(0, 3))
        val suggestion = CodeSuggestion("println", CodeSuggestion.Kind.FUNCTION, "println()", range = replaceRange)

        val shown = CodeEditorReducer.reduce(
            state, CodeEditorAction.Completion.Show(listOf(suggestion), TextPosition(0, 3))
        ).state

        val applied = CodeEditorReducer.reduce(shown, CodeEditorAction.Completion.Apply)
        assertEquals(CodeEditorState.CompletionState.Hidden, applied.state.completion)

        val cmd = applied.command as CodeEditorCommand.ApplyText
        val replaceOp = cmd.operation as TextOperation.Data.Single.Replace
        assertEquals(replaceRange, replaceOp.range)
        assertEquals("println()", replaceOp.text)
    }

    @Test
    fun `toggleComment comments and uncomments multiline block using minimum indent`() {
        val code = "    val a = 1\n  val b = 2"
        val selection = Selection.fromPositions(TextPosition(0, 0), TextPosition(1, 5))
        val state = createState(code, listOf(selection))

        val commentResult = CodeEditorReducer.reduce(state, CodeEditorAction.Edit.ToggleComment("// "))
        val commentCmd = commentResult.command as CodeEditorCommand.ApplyText
        val commentBatch = commentCmd.operation as TextOperation.Data.Batch

        val opLine1 = commentBatch.operations[0] as TextOperation.Data.Single.Insert
        val opLine0 = commentBatch.operations[1] as TextOperation.Data.Single.Insert
        assertEquals(TextPosition(1, 2), opLine1.position)
        assertEquals(TextPosition(0, 2), opLine0.position)

        val commentedState = createState("  //   val a = 1\n  // val b = 2", listOf(selection))
        val uncommentResult = CodeEditorReducer.reduce(commentedState, CodeEditorAction.Edit.ToggleComment("// "))
        val uncommentCmd = uncommentResult.command as CodeEditorCommand.ApplyText
        val uncommentBatch = uncommentCmd.operation as TextOperation.Data.Batch

        assertTrue(uncommentBatch.operations.all { it is TextOperation.Data.Single.Delete })
    }

    @Test
    fun `updateSyntax populates folding ranges and initially collapsed lines`() {
        val state = createState("line0\nline1\nline2\nline3")
        val syntax = Syntax(
            revision = TextRevision(1L), foldingRegions = listOf(
                FoldingRegion.Expanded(TextRange(TextPosition(0, 0), TextPosition(1, 5))),
                FoldingRegion.Collapsed(TextRange(TextPosition(2, 0), TextPosition(3, 5))),
            ), occurrences = emptyList(), tokensPerLine = emptyMap()
        )

        val updated = CodeEditorReducer.reduce(state, CodeEditorAction.External.UpdateSyntax(syntax)).state
        assertEquals(listOf(0..1, 2..3), updated.editor.collapsedRanges)
        assertTrue(2 in updated.editor.collapsedLines)
        assertFalse(0 in updated.editor.collapsedLines)
    }

    @Test
    fun `moving caret dismisses active completion overlay`() {
        val state = createState()
        val suggestions = listOf(CodeSuggestion("item", CodeSuggestion.Kind.VARIABLE, "item"))
        val showResult = CodeEditorReducer.reduce(
            state, CodeEditorAction.Completion.Show(suggestions, TextPosition(0, 0))
        )

        assertTrue(showResult.state.completion is CodeEditorState.CompletionState.Visible)

        val moveResult = CodeEditorReducer.reduce(
            showResult.state, CodeEditorAction.Core(CaretAction.Move(TextPosition(0, 0)))
        )

        assertEquals(CodeEditorState.CompletionState.Hidden, moveResult.state.completion)
    }
}