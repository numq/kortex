package io.github.numq.kortex.example.shared

import io.github.numq.kortex.code.token.Token
import io.github.numq.kortex.code.token.TokenProvider
import io.github.numq.kortex.code.token.TokenType
import io.github.numq.krope.text.TextPosition
import io.github.numq.krope.text.TextRange
import io.github.numq.krope.text.TextSnapshot

class KotlinTokenProvider : TokenProvider {
    private val keywords = setOf(
        "package",
        "import",
        "class",
        "interface",
        "fun",
        "val",
        "var",
        "return",
        "if",
        "else",
        "when",
        "for",
        "while",
        "do",
        "try",
        "catch",
        "finally",
        "throw",
        "object",
        "companion",
        "sealed",
        "data",
        "enum",
        "override",
        "open",
        "private",
        "protected",
        "public",
        "internal",
        "abstract",
        "actual",
        "expect",
        "inline"
    )

    private val builtinTypes = setOf(
        "Int",
        "Long",
        "Float",
        "Double",
        "Boolean",
        "String",
        "Char",
        "Byte",
        "Short",
        "Unit",
        "Any",
        "Nothing",
        "Array",
        "List",
        "Map",
        "Set",
        "ByteArray"
    )

    override suspend fun provideTokens(snapshot: TextSnapshot, range: TextRange?): Map<Int, List<Token>> {
        val startLine = range?.start?.line ?: 0
        val endLine = range?.end?.line ?: (snapshot.lines - 1)
        val result = HashMap<Int, List<Token>>(endLine - startLine + 1)

        var insideBlockComment = false

        for (line in 0 until snapshot.lines) {
            val text = snapshot.getLineText(line)
            val lineTokens = mutableListOf<Token>()
            var col = 0
            val len = text.length

            while (col < len) {
                if (insideBlockComment) {
                    val endComment = text.indexOf("*/", col)
                    if (endComment != -1) {
                        lineTokens.add(
                            Token(
                                range = TextRange(TextPosition(line, col), TextPosition(line, endComment + 2)),
                                type = TokenType.COMMENT_BLOCK
                            )
                        )
                        col = endComment + 2
                        insideBlockComment = false
                        continue
                    } else {
                        lineTokens.add(
                            Token(
                                range = TextRange(TextPosition(line, col), TextPosition(line, len)),
                                type = TokenType.COMMENT_BLOCK
                            )
                        )
                        break
                    }
                }

                val ch = text[col]

                if (ch.isWhitespace()) {
                    col++
                    continue
                }

                if (ch == '/' && col + 1 < len) {
                    when (text[col + 1]) {
                        '/' -> {
                            lineTokens.add(
                                Token(
                                    range = TextRange(TextPosition(line, col), TextPosition(line, len)),
                                    type = TokenType.COMMENT_LINE
                                )
                            )
                            break
                        }

                        '*' -> {
                            insideBlockComment = true
                            val endComment = text.indexOf("*/", col + 2)
                            if (endComment != -1) {
                                lineTokens.add(
                                    Token(
                                        range = TextRange(TextPosition(line, col), TextPosition(line, endComment + 2)),
                                        type = TokenType.COMMENT_BLOCK
                                    )
                                )
                                col = endComment + 2
                                insideBlockComment = false
                            } else {
                                lineTokens.add(
                                    Token(
                                        range = TextRange(TextPosition(line, col), TextPosition(line, len)),
                                        type = TokenType.COMMENT_BLOCK
                                    )
                                )
                                break
                            }
                            continue
                        }
                    }
                }

                if (ch == '"') {
                    val isTriple = col + 2 < len && text[col + 1] == '"' && text[col + 2] == '"'
                    val startCol = col
                    if (isTriple) {
                        col += 3
                        val endTriple = text.indexOf("\"\"\"", col)
                        if (endTriple != -1) {
                            col = endTriple + 3
                        } else {
                            col = len
                        }
                    } else {
                        col++
                        while (col < len) {
                            if (text[col] == '\\' && col + 1 < len) {
                                col += 2
                            } else if (text[col] == '"') {
                                col++
                                break
                            } else {
                                col++
                            }
                        }
                    }
                    lineTokens.add(
                        Token(
                            range = TextRange(TextPosition(line, startCol), TextPosition(line, col)),
                            type = TokenType.STRING
                        )
                    )
                    continue
                }

                if (ch.isDigit()) {
                    val startCol = col
                    while (col < len && (text[col].isDigit() || text[col] in "xXabcdefABCDEF_lLfF.")) {
                        col++
                    }
                    lineTokens.add(
                        Token(
                            range = TextRange(TextPosition(line, startCol), TextPosition(line, col)),
                            type = TokenType.NUMBER
                        )
                    )
                    continue
                }

                if (ch.isLetter() || ch == '_') {
                    val startCol = col
                    while (col < len && (text[col].isLetterOrDigit() || text[col] == '_')) {
                        col++
                    }
                    val word = text.substring(startCol, col)

                    var probe = col
                    while (probe < len && text[probe].isWhitespace()) probe++
                    val isFunctionCall = probe < len && text[probe] == '('

                    val tokenType = when {
                        word in keywords -> TokenType.KEYWORD
                        word in builtinTypes -> TokenType.TYPE_BUILTIN
                        isFunctionCall -> TokenType.FUNCTION
                        ch.isUpperCase() -> TokenType.TYPE
                        else -> TokenType.VARIABLE
                    }

                    lineTokens.add(
                        Token(
                            range = TextRange(TextPosition(line, startCol), TextPosition(line, col)), type = tokenType
                        )
                    )
                    continue
                }

                if (ch in "{}()[]") {
                    lineTokens.add(
                        Token(
                            range = TextRange(TextPosition(line, col), TextPosition(line, col + 1)),
                            type = TokenType.PUNCTUATION_BRACKET
                        )
                    )
                    col++
                    continue
                }

                if (ch in "+-*/%=<>!&|^?:.~") {
                    val startCol = col
                    while (col < len && text[col] in "+-*/%=<>!&|^?:.~") {
                        col++
                    }
                    lineTokens.add(
                        Token(
                            range = TextRange(TextPosition(line, startCol), TextPosition(line, col)),
                            type = TokenType.OPERATOR
                        )
                    )
                    continue
                }

                if (ch in ";,") {
                    lineTokens.add(
                        Token(
                            range = TextRange(TextPosition(line, col), TextPosition(line, col + 1)),
                            type = TokenType.PUNCTUATION_DELIMITER
                        )
                    )
                    col++
                    continue
                }

                col++
            }

            if (line in startLine..endLine) {
                result[line] = lineTokens
            }
        }

        return result
    }
}