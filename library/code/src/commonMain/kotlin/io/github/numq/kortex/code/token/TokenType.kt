package io.github.numq.kortex.code.token

import kotlin.jvm.JvmInline

@JvmInline
value class TokenType(val name: String) {
    companion object {
        val KEYWORD = TokenType("keyword")
        val KEYWORD_CONTROL = TokenType("keyword.control")
        val TYPE = TokenType("type")
        val TYPE_BUILTIN = TokenType("type.builtin")
        val FUNCTION = TokenType("function")
        val FUNCTION_METHOD = TokenType("function.method")
        val VARIABLE = TokenType("variable")
        val VARIABLE_PARAMETER = TokenType("variable.parameter")
        val STRING = TokenType("string")
        val NUMBER = TokenType("number")
        val COMMENT = TokenType("comment")
        val COMMENT_LINE = TokenType("comment.line")
        val COMMENT_BLOCK = TokenType("comment.block")
        val OPERATOR = TokenType("operator")
        val PUNCTUATION = TokenType("punctuation")
        val PUNCTUATION_BRACKET = TokenType("punctuation.bracket")
        val PUNCTUATION_DELIMITER = TokenType("punctuation.delimiter")
        val MARKUP_LINK = TokenType("markup.link")
        val DEFAULT = TokenType("default")
    }
}