# Kortex

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-purple.svg?logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Skia Powered](https://img.shields.io/badge/Rendering-Skia%20%2F%20Skiko-critical.svg)](https://skia.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

**Kortex** is an ultra-fast, hardware-accelerated code and text editor component built for **Compose Multiplatform**
(Desktop JVM, Android, iOS, and Wasm/JS).

Engineered from the ground up for high throughput, sub-millisecond input latency, and 60+ FPS scrolling on massive
documents, Kortex bypasses the standard overhead of composable text trees by coupling a **Red-Black Rope persistent text
buffer (`Krope`)** directly with a virtualized **Skia/Skiko rendering pipeline** and a deterministic **unidirectional
MVI reducer architecture**.

---

## Table of Contents

- [Key Highlights](#key-highlights)
- [Architecture & Design Principles](#architecture--design-principles)
- [Module Structure](#module-structure)
- [Supported Platforms](#supported-platforms)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Basic Code Editor Setup](#basic-code-editor-setup)
- [Deep Dive & Customization](#deep-dive--customization)
    - [1. Implementing Language Support](#1-implementing-language-support)
        - [Syntax Highlighting (`TokenProvider`)](#syntax-highlighting-tokenprovider)
        - [Diagnostics & Squiggles (`DiagnosticsProvider`)](#diagnostics--squiggles-diagnosticsprovider)
        - [Code Completion (`CompletionProvider`)](#code-completion-completionprovider)
    - [2. Theming & Token Styling](#2-theming--token-styling)
    - [3. Keymap & Custom Keybindings](#3-keymap--custom-keybindings)
    - [4. Viewport Virtualization & Code Folding](#4-viewport-virtualization--code-folding)
    - [5. History & Smart Undo/Redo Merging](#5-history--smart-undoredo-merging)
    - [6. Multi-Cursor & Selection Normalization](#6-multi-cursor--selection-normalization)
- [API Overview](#api-overview)
- [License](#license)

---

## Key Highlights

- **⚡ Hardware-Accelerated Skia Direct Rendering**: Bypasses heavy composable layout trees. Text, carets, selection
  rectangles, squiggly diagnostics, rulers, and gutter markers are drawn directly to an accelerated Skia `Canvas` in
  dedicated virtualized layers.
- **🌲 Backed by Krope (Red-Black Rope Buffer)**: Immutable persistent text snapshots, $O (\log N)$ insertions,
  deletions, and slice operations, minimal memory footprint, and zero-allocation substring slices.
- **🔄 Deterministic MVI State Reducer**: Pure reduction functions (`(State, Action) -> Result(State, Command)`) govern
  every caret step, multi-selection update, line fold, and text modification.
- **✨ Full Multi-Cursor & Multi-Selection Support**: Multiple non-overlapping carets, rectangular-like selection
  dragging (via Alt/Option), and VS Code-style `Select Next Match` (`Ctrl+D` / `Cmd+D`) with automatic interval
  normalization.
- **🔎 Rich Language Tooling Pipeline**:
    - Asynchronous, debounced tokenization flow (`50ms`).
    - Debounced diagnostics pipeline (`300ms`) with custom sine-wave/zigzag squiggles (Error, Warning, Info, Hint).
    - Code completion overlay with keyboard navigation (`Arrows`, `Enter`/`Tab`, `Escape`).
    - Symbol occurrence highlighting synchronized with the active caret.
- **📂 High-Performance Code Folding (`FoldMap`)**: $O (\log K)$ binary search mapping between logical document lines and
  visual viewport lines, supporting collapsible intervals.
- **🎯 Intelligent History Management**: Undo/Redo stack with automatic debounce merging for contiguous typing and
  backspacing (within a 700ms window) and selection state restoration.
- **🎨 Flexible Hierarchical Theming**: Fully decoupled syntax theme system matching hierarchical scopes (e.g.,
  `keyword.control` falling back to `keyword`), with out-of-the-box Material 3 Dark and Light color schemes.
- **🧩 Custom Keymap DSL**: Expressive DSL (`buildEditorKeymap`) to rebind actions, add chord shortcuts, and manage
  read-only constraints.
- **🖱️ Native Desktop Enhancements**: Scrollbar adapters (`asVerticalScrollbarAdapter`, `asHorizontalScrollbarAdapter`),
  right-click context menu (Cut, Copy, Paste, Undo, Redo, Select All), and line-number gutter click actions.

---

## Architecture & Design Principles

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           UI / Compose Layer                            │
│  KortexCodeEditor / KortexEditor  ◄──► EditorScrollController (Virtual) │
│       │                                                  │              │
│  Skia Direct Canvas Rendering ◄── LayerFactory (SkiaLayerFactory)       │
│  [Background, CurrentLine, Gutter, Content, Selection, Caret, Diags]     │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Dispatches Actions
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          Engine & Reducer Core                          │
│                                                                         │
│   CodeEditorEngine / EditorEngine                                       │
│       │                                                                 │
│   EditorReducer (Pure Functional)                                       │
│   ├── EditReducer                                                       │
│   ├── CaretReducer                                                      │
│   ├── SelectionReducer                                                  │
│   ├── ViewportReducer & FoldMap                                         │
│   └── HistoryReducer                                                    │
│       │                                                                 │
│       ▼                                                                 │
│   TextOperation.Data (Insert / Delete / Replace / Batch)                │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Executes Operations
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       Persistent Text Buffer (Krope)                    │
│   RopeTextBuffer  ──► TextSnapshot (Immutable Revision, Lines, Chars)   │
└─────────────────────────────────────────────────────────────────────────┘
```

1. **Unidirectional Data Flow**: The UI observes an immutable `StateFlow<EditorState>` or `StateFlow<CodeEditorState>`.
   Any user gesture (key event, pointer drag, scroll) produces an `EditorAction` or `CodeEditorAction`.
2. **Separation of Text Buffer from Editor State**: Text mutations are expressed as `TextOperation.Data` and executed
   atomically on `RopeTextBuffer`. The resulting `TextEdit.Data` deltas are projected onto carets and selections so
   cursor positions survive batch operations effortlessly.
3. **Multi-Level LRU Caching**: Skia `Paragraph`, `TextLine`, and `Paint` instances are cached in thread-safe LRU caches
   (`LruCache`), avoiding repetitive paragraph allocations during 60 FPS scrolling and typing.

---

## Module Structure

The project is structured into modular layers, separating core text manipulation from Compose UI bindings and IDE-grade
language features:

| Module                 | Description                                                                                                                                                     |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `:kortex-core`         | Low-level editor primitives: `EditorEngine`, `EditorState`, `Selection`, `Caret`, `FoldMap`, `HistoryManager`, and reducers. Fully independent of Compose UI.   |
| `:kortex-core-compose` | Skia canvas rendering integration (`SkiaCanvas`), `EditorFont`, `FontManager`, `EditorScrollController`, `EditorInputArea`, keymaps, and default context menus. |
| `:kortex-code`         | Code-centric abstractions: `CodeEditorEngine`, `LanguageAnalysisService`, tokens, diagnostics, occurrences, and completion engine.                              |
| `:kortex-code-compose` | High-level `KortexCodeEditor` composable, syntax highlighting layers, folding markers, column ruler, and completion overlay.                                    |

---

## Supported Platforms

Kortex uses Kotlin Multiplatform and Skiko/Skia bindings:

- **Desktop JVM** (macOS, Linux, Windows)
- **Android** (via Skiko `SkikoSurfaceView`)
- **iOS** (`iosX64`, `iosArm64`, `iosSimulatorArm64`)
- **Web** (`wasmJs`)

---

## Getting Started

### Prerequisites

Ensure you have:

- Kotlin `2.0+`
- Compose Multiplatform `1.6+`
- `Krope` text buffer library

### Basic Code Editor Setup

Here is a minimal example setting up a high-performance code editor in Compose Multiplatform:

```kotlin
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

@Composable
fun CodeEditorScreen() {
    val scope = rememberCoroutineScope()

    // 1. Initialize persistent Rope buffer
    val buffer = remember {
        RopeTextBuffer(
            initialText = """
                fun main() {
                    println("Hello, Kortex Skia Editor!")
                }
            """.trimIndent(),
            initialLineEnding = TextLineEnding.LF,
            initialEncoding = Encoding.UTF8,
            enablePooling = true
        )
    }

    // 2. Instantiate core & code engines
    val editorEngine = remember(buffer) {
        EditorEngine(scope = scope, buffer = buffer)
    }
    val codeEngine = remember(editorEngine) {
        CodeEditorEngine(
            scope = scope,
            engine = editorEngine,
            analysisService = object : LanguageAnalysisService {} // plug your language services here
        )
    }

    // 3. Configure typography & theme
    val font = remember {
        val typeface = FontMgr.default.matchFamilyStyle("JetBrains Mono", FontStyle.NORMAL)
            ?: FontMgr.default.matchFamilyStyle(null, FontStyle.NORMAL)
            ?: Typeface.makeEmpty()
        EditorFont(typeface = typeface, size = 14f, lineSpacing = 1.25f)
    }

    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val theme = remember(colorScheme) { CodeEditorTheme.fromColorScheme(colorScheme) }
    val scrollController = rememberEditorScrollController()

    // 4. Render editor with native scrollbars
    MaterialTheme(colorScheme = colorScheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            KortexCodeEditor(
                engine = codeEngine,
                font = font,
                theme = theme,
                scrollController = scrollController,
                columnRuler = 80,
                modifier = Modifier.fillMaxSize()
            )

            // Optional: Desktop scrollbars
            if (scrollController.canScrollVertically) {
                VerticalScrollbar(
                    adapter = rememberVerticalScrollbarAdapter(scrollController),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
            if (scrollController.canScrollHorizontally) {
                val density = LocalDensity.current
                val gutterPadding = with(density) { scrollController.gutterWidth.toDp() }
                HorizontalScrollbar(
                    adapter = rememberHorizontalScrollbarAdapter(scrollController),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = gutterPadding)
                )
            }
        }
    }
}
```

---

## Deep Dive & Customization

### 1. Implementing Language Support

To empower Kortex with language intelligence (syntax highlighting, diagnostics, completions, tooltips), implement
`LanguageAnalysisService`:

```kotlin
val myLanguageService = object : LanguageAnalysisService {
    override val tokenProvider: TokenProvider = TokenProvider { snapshot, range ->
        // Return tokens mapped per line index: Map<Int, List<Token>>
        // Runs asynchronously and debounced at 50ms on background thread
        computeTokens(snapshot)
    }

    override val diagnosticsProvider: DiagnosticsProvider = DiagnosticsProvider { snapshot ->
        // Return diagnostics: List<CodeIssue>
        // Runs asynchronously and debounced at 300ms
        computeDiagnostics(snapshot)
    }

    override val completionProvider: CompletionProvider = CompletionProvider { snapshot, offset ->
        // Return auto-completion suggestions: List<CodeSuggestion>
        // Triggered via Ctrl+Space / Cmd+Space or programmatic request
        computeCompletions(snapshot, offset)
    }

    override val documentationProvider: DocumentationProvider = DocumentationProvider { snapshot, offset ->
        // Quick info / documentation popups
        computeDocs(snapshot, offset)
    }
}
```

#### Syntax Highlighting (`TokenProvider`)

Tokens define a `TextRange` and a `TokenType`:

```kotlin
val token = Token(
    range = TextRange(start = TextPosition(line = 0, column = 0), end = TextPosition(line = 0, column = 3)),
    type = TokenType.KEYWORD
)
```

Predefined token types include:

- `TokenType.KEYWORD`, `TokenType.KEYWORD_CONTROL`
- `TokenType.TYPE`, `TokenType.TYPE_BUILTIN`
- `TokenType.FUNCTION`, `TokenType.FUNCTION_METHOD`
- `TokenType.VARIABLE`, `TokenType.VARIABLE_PARAMETER`
- `TokenType.STRING`, `TokenType.NUMBER`
- `TokenType.COMMENT`, `TokenType.COMMENT_LINE`, `TokenType.COMMENT_BLOCK`
- `TokenType.OPERATOR`, `TokenType.PUNCTUATION`, `TokenType.PUNCTUATION_BRACKET`, `TokenType.PUNCTUATION_DELIMITER`
- `TokenType.MARKUP_LINK`, `TokenType.DEFAULT`

#### Diagnostics & Squiggles (`DiagnosticsProvider`)

Issues are rendered as smooth, wavy underline paths beneath the faulty text span:

```kotlin
val error = CodeIssue.Error(
    range = TextRange(TextPosition(line = 2, column = 4), TextPosition(line = 2, column = 12)),
    message = "Unresolved reference: foo",
    source = "Compiler",
    code = "E001"
)
```

Available issue severities: `CodeIssue.Error`, `CodeIssue.Warning`, `CodeIssue.Information`, `CodeIssue.Hint`, and
`CodeIssue.Unknown`.

#### Code Completion (`CompletionProvider`)

```kotlin
val suggestion = CodeSuggestion(
    label = "println(message)",
    kind = CodeSuggestion.Kind.FUNCTION,
    text = "println()",
    detail = "Unit",
    documentation = "Prints the given message to standard output."
)
```

When completion popup is open:

- `Up` / `Down`: Navigate suggestions.
- `Enter` / `Tab`: Apply suggestion.
- `Escape`: Dismiss completion popup.

---

### 2. Theming & Token Styling

Kortex provides a hierarchical fallback resolver. If you define a style for `keyword`, it will automatically format
`keyword.control` unless explicitly overridden:

```kotlin
val customSyntaxTheme = SyntaxTheme.build(
    defaultText = Color(0xFFABB2BF)
) {
    put(TokenType.KEYWORD.name, Color(0xFFC678DD))
    put(TokenType.KEYWORD_CONTROL.name, Color(0xFFE06C75))
    put(TokenType.FUNCTION.name, Color(0xFF61AFEF))
    put(TokenType.STRING.name, Color(0xFF98C379))
    put(TokenType.NUMBER.name, Color(0xFFD19A66))
    put(TokenType.COMMENT.name, Color(0xFF5C6370))
}

val customEditorTheme = CodeEditorTheme(
    editor = EditorTheme.fromColorScheme(colorScheme),
    syntax = customSyntaxTheme,
    diagnostics = DiagnosticsTheme(
        error = Color(0xFFE06C75),
        warning = Color(0xFFE5C07B),
        info = Color(0xFF61AFEF),
        hint = Color(0xFF5C6370)
    ),
    occurrences = OccurrencesTheme(
        matchBackground = Color(0xFF3E4451).copy(alpha = 0.5f),
        currentMatchBackground = Color(0xFF528BFF).copy(alpha = 0.4f)
    ),
    folding = FoldingTheme(
        iconColor = Color(0xFF61AFEF)
    ),
    ruler = Color(0xFF3E4451).copy(alpha = 0.3f)
)
```

---

### 3. Keymap & Custom Keybindings

You can extend or modify editor keybindings using `buildEditorKeymap`:

```kotlin
val customKeymap = buildEditorKeymap(baseKeymap = DefaultCodeEditorKeymap.create(codeEngine)) {
    // Save shortcut: Ctrl+S / Cmd+S
    bind(Key.S, primary = true, description = "Save Document") {
        // Access engine or external logic
        val fullText = state.snapshot.text
        saveToFile(fullText)
    }

    // Move line up: Alt + Up
    bind(Key.DirectionUp, alt = true, requiresEditable = true, description = "Move Line Up") {
        dispatch(EditAction.MoveLineUp)
    }

    // Format code: Ctrl+Alt+L
    bind(Key.L, primary = true, alt = true, requiresEditable = true, description = "Reformat Code") {
        reformat()
    }
}
```

#### Default Shortcut Cheat Sheet

| Shortcut                         | Description                                        |
|----------------------------------|----------------------------------------------------|
| `Ctrl+A` / `Cmd+A`               | Select All                                         |
| `Ctrl+D` / `Cmd+D`               | Select Word / Select Next Match (Multi-Cursor)     |
| `Alt + Click / Drag`             | Add new cursor / multi-cursor selection            |
| `Ctrl+Z` / `Cmd+Z`               | Undo                                               |
| `Ctrl+Shift+Z` / `Cmd+Shift+Z`   | Redo                                               |
| `Ctrl+C` / `Ctrl+X` / `Ctrl+V`   | Copy / Cut / Paste                                 |
| `Ctrl+Shift+D`                   | Duplicate Line or Selection                        |
| `Alt + Up / Down`                | Move current line/selection up or down             |
| `Tab` / `Shift+Tab`              | Indent / Unindent selection (or soft tabs)         |
| `Ctrl+/` / `Cmd+/`               | Toggle Line Comment (`// `)                        |
| `Ctrl+Space`                     | Request Code Completion                            |
| `Ctrl+Backspace` / `Ctrl+Delete` | Delete Word Left / Right                           |
| `Home` / `End`                   | Move to line start (smart indent aware) / line end |
| `Ctrl+Home` / `Ctrl+End`         | Move to document start / end                       |
| `PageUp` / `PageDown`            | Smooth page scrolling with caret tracking          |
| `Escape`                         | Clear extra cursors or dismiss completion overlay  |

---

### 4. Viewport Virtualization & Code Folding

Large files with tens of thousands of lines render without stutter thanks to virtualized line measurement:

```kotlin
// In EditorState:
val foldMap = state.foldMap
val visualLine = foldMap.documentToVisualLine(docLine)
val documentLine = foldMap.visualToDocumentLine(visualLine)
```

- **`FoldMap`** computes cumulative hidden line counts using interval trees with $O (\log K)$ binary search lookup.
- Lines inside collapsed folding regions are skipped entirely during Skia canvas rendering and coordinate resolution.
- Clicking the folding chevron in the gutter dispatches `ViewportAction.ToggleFolding(line)`.

---

### 5. History & Smart Undo/Redo Merging

Typing individual characters doesn't pollute the undo stack with hundreds of single-letter steps. `HistoryManager`
automatically merges edits when:

1. Operations are of the same type (`Insert` + `Insert` or `Delete` + `Delete`).
2. Edits occur consecutively within **700ms**.
3. No newline characters are inserted.
4. The caret positions align contiguously.

On Undo/Redo, selections and cursor locations are fully restored to their exact state prior to the action.

---

### 6. Multi-Cursor & Selection Normalization

Multiple cursors can be added programmatically or interactively (via `Alt+Click` / `Alt+Drag` or `SelectNextMatch`):

```kotlin
// Add a cursor at line 5, column 10
editorEngine.dispatch(CaretAction.AddCursor(TextPosition(line = 5, column = 10)))

// Multi-cursor duplicate selection
editorEngine.dispatch(SelectionAction.SelectNextMatch)
```

Selections are automatically **normalized**:

- Overlapping ranges are coalesced into a single unified range.
- Selections maintain anchor, caret, and direction (`FORWARD` / `BACKWARD`).
- Sticky column positions ensure consistent vertical navigation across variable-length lines.

---

## API Overview

### Core Types

- `EditorEngine`: Core state holder coordinating the text buffer, undo history, and reducer dispatch loop.
- `CodeEditorEngine`: Wraps `EditorEngine` with asynchronous debounced analysis pipelines (`Syntax`, `Analysis`,
  `Completion`).
- `EditorState`: Immutable snapshot of the editor state (`snapshot`, `selections`, `collapsedLines`, `collapsedRanges`,
  `config`).
- `CodeEditorState`: Extends editor state with syntax tokens, code diagnostics, and auto-completion overlay state.
- `RopeTextBuffer`: Persistent Red-Black rope text buffer with structural sharing and lock-free snapshot access.
- `EditorScrollController`: Controls horizontal and vertical viewport offsets, virtual sizes, and caret tracking.
- `EditorFont`: Thread-safe Skia typeface wrapper managing subpixel metrics, ascents, descents, and paragraph builders.

### Actions

- `EditAction`: `Insert`, `Paste`, `Backspace`, `Delete`, `Enter`, `Tab`, `Untab`, `MoveLineUp`, `MoveLineDown`,
  `Duplicate`, `WordDeleteLeft`, `WordDeleteRight`.
- `CaretAction`: `Move`, `AddCursor`, `MoveLeft`, `MoveRight`, `MoveUp`, `MoveDown`, `MoveWordLeft`, `MoveWordRight`,
  `MoveLineStart`, `MoveLineEnd`, `MoveDocStart`, `MoveDocEnd`.
- `SelectionAction`: `Clear`, `SelectAll`, `SelectWordAt`, `SelectLine`, `SelectRange`, `AddSelection`, `SetSelections`,
  `SelectNextMatch`.
- `ViewportAction`: `ToggleFolding`, `UpdateCollapsedLines`, `UpdateFoldingRegions`.
- `HistoryAction`: `Undo`, `Redo`.
- `CodeEditorAction`: `Core`, `Edit.ToggleComment`, `External.UpdateSyntax`, `External.UpdateAnalysis`,
  `Completion.Show`, `Completion.Apply`, `Completion.Dismiss`, `Completion.SelectNext`, `Completion.SelectPrevious`.

---

## License

Apache-2.0 License - see [LICENSE](LICENSE) file for details.

---

<p align="center">
  <a href="https://numq.github.io/support">
    <img src="https://api.qrserver.com/v1/create-qr-code/?size=112x112&data=https://numq.github.io/support&bgcolor=1a1b26&color=7aa2f7" 
         width="112" 
         height="112" 
         style="border-radius: 4px;" 
         alt="Support QR code">
  </a>
  <br>
  <a href="https://numq.github.io/support" style="text-decoration: none;">
    <code><font color="#bb9af7">Support Development: numq.github.io/support</font></code>
  </a>
</p>
