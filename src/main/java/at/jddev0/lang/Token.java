package at.jddev0.lang;

import java.util.Objects;

/**
 * Lang-Module<br>
 * Token for Lang
 *
 * @author JDDev0
 * @version v1.0.0
 */
public class Token {
    public final CodePosition pos;
    public final String value;
    public final TokenType tokenType;

    public Token(int lineNumberFrom, int lineNumberTo, int columnFrom, int columnTo, String value, TokenType tokenType) {
        this.pos = new CodePosition(lineNumberFrom, lineNumberTo, columnFrom, columnTo);
        this.value = value;
        this.tokenType = tokenType;
    }

    public Token(CodePosition pos, String value, TokenType tokenType) {
        this.pos = pos;
        this.value = value;
        this.tokenType = tokenType;
    }

    public CodePosition getPos() {
        return pos;
    }

    public String getValue() {
        return value;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if(!(obj instanceof Token))
            return false;

        Token that = (Token)obj;
        return Objects.equals(pos, that.pos) && Objects.equals(value, that.value) &&
                tokenType == that.tokenType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, value, tokenType);
    }

    @Override
    public String toString() {
        return String.format("Token (%30s at %s): \"%s\"", tokenType.name(), pos.toString(), value);
    }

    public String toRawString() {
        if(tokenType == TokenType.LEXER_ERROR)
            return "";

        return value;
    }

    public enum TokenType {
        OTHER, //TODO Improve

        LITERAL_NULL,
        LITERAL_TEXT,
        LITERAL_NUMBER,

        ARGUMENT_SEPARATOR,

        ESCAPE_SEQUENCE,

        PARSER_FUNCTION_IDENTIFIER,

        IDENTIFIER,

        OPERATOR,

        ASSIGNMENT,

        OPENING_BRACKET,
        CLOSING_BRACKET,

        SINGLE_LINE_TEXT_QUOTES,

        START_MULTILINE_TEXT,
        END_MULTILINE_TEXT,

        START_COMMENT,
        START_DOC_COMMENT,
        END_COMMENT,

        LINE_CONTINUATION,

        WHITESPACE,
        EOL,
        EOF,

        LEXER_ERROR
    }
}
