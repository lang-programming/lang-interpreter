package at.jddev0.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Lang-Module<br>
 * Lexing of Lang files into a token structure for the LangParser
 *
 * @author JDDev0
 * @version v1.0.0
 */
public final class LangLexer {
    private static final List<String> OPERATORS = Arrays.asList(
            /* Array unpacking */ "...",
            "!==", "!=~", "!=", "===", "=~", "==", "<=>", "<=", ">=", "|||", "&&", "||", "!", "&", "~~", "~/", "~",
            "\u25b2", "\u25bc", "**", "*", "//", "^/", "/", "%", "^", "|", "<<", ">>>", ">>", "+|", "->", "-|", "+", "-", "@", "?::",
            "?:", "<", ">", "??", /* Optional get item */ "?.", "::",
            /* Inline-if (1st part) */ "?",
            /* Inline-if (2nd part) */ ":"
    );

    private int lineNumber;
    private int column;

    private int openingBracketCount;
    private int openingBlockCount;

    private boolean linesIsEmpty;
    private boolean isFirstCodeTokenInLine = true;

    public LangLexer() {
        resetPositionVars();
    }

    public void resetPositionVars() {
        lineNumber = 1;
        column = 1;

        openingBracketCount = 0;
        openingBlockCount = 0;

        linesIsEmpty = false;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getOpeningBracketCount() {
        return openingBracketCount;
    }

    public int getOpeningBlockCount() {
        return openingBlockCount;
    }

    public List<Token> readTokens(BufferedReader reader) throws IOException {
        if(reader == null)
            return null;

        linesIsEmpty = false;

        List<String> lines = reader.lines().collect(Collectors.toList());

        List<Token> tokens = new LinkedList<>();

        if(!lines.isEmpty()) {
            String currentLine = lines.remove(0);
            while(currentLine != null)
                currentLine = tokenizeNextTokens(currentLine, lines, tokens);
        }

        tokens.add(new Token(lineNumber, lineNumber, column, column, "", Token.TokenType.EOF));

        return tokens;
    }

    private String tokenizeNextTokens(String currentLine, List<String> lines, List<Token> tokens) {
        String ret;

        boolean wasLinesEmpty = lines.isEmpty();
        ret = tryTokenizeNewLine(currentLine, lines, tokens);
        if(ret != null || linesIsEmpty) {
            if(linesIsEmpty || (ret.isEmpty() && wasLinesEmpty))
                return null;

            isFirstCodeTokenInLine = true;
            return ret;
        }

        ret = tryTokenizeTokens(currentLine, lines, tokens);
        if(ret != null)
            return ret;

        //Tokenize as OTHER if not matched with anything else

        int tokenIndex = tokens.size();
        int fromColumn = column;
        int fromLineNumber = lineNumber;

        int i = 0;
        while(i < currentLine.length()) {
            //Skip parsing of "+" and "-" if floating point number contains an "e" or an "E"
            if(!LangPatterns.matches(currentLine.substring(0, i), LangPatterns.PARSING_FLOATING_POINT_E_SYNTAX_START) ||
                    (currentLine.charAt(i) != '+' && currentLine.charAt(i) != '-')) {
                ret = tryTokenizeTokens(currentLine.substring(i), lines, tokens);
                if(ret != null) {
                    String token = currentLine.substring(0, i);
                    tokens.add(tokenIndex, tokenizeOtherValue(token, new CodePosition(fromLineNumber, fromLineNumber,
                            fromColumn, fromColumn + i)));

                    return ret;
                }
            }

            column++;
            i++;
        }

        tokens.add(tokenIndex, tokenizeOtherValue(currentLine, new CodePosition(fromLineNumber, fromLineNumber,
                fromColumn, column)));

        return "";
    }

    private String tryTokenizeTokens(String currentLine, List<String> lines, List<Token> tokens) {
        String ret;

        ret = tryTokenizeMultilineText(currentLine, lines, tokens);
        if(ret != null) {
            isFirstCodeTokenInLine = false;

            return ret;
        }

        ret = tryTokenizeLineContinuation(currentLine, lines, tokens);
        if(ret != null)
            return ret;

        ret = tryTokenizeEscapeSequence(currentLine, lines, tokens);
        if(ret != null) {
            isFirstCodeTokenInLine = false;

            return ret;
        }

        ret = tryTokenizeSingleLineText(currentLine, lines, tokens);
        if(ret != null) {
            isFirstCodeTokenInLine = false;

            return ret;
        }

        ret = tryTokenizeAssignment(currentLine, lines, tokens);
        if(ret != null) {
            isFirstCodeTokenInLine = false;

            return ret;
        }

        ret = tryTokenizeArgumentSeparator(currentLine, lines, tokens);
        if(ret != null) {
            isFirstCodeTokenInLine = false;

            return ret;
        }

        ret = tryTokenizeWhitespace(currentLine, lines, tokens);
        if(ret != null)
            return ret;

        ret = tryTokenizeComment(currentLine, lines, tokens);
        if(ret != null)
            return ret;

        ret = tryTokenizeParserFunctionIdentifier(currentLine, lines, tokens);
        if(ret != null) {
            isFirstCodeTokenInLine = false;

            return ret;
        }

        ret = tryTokenizeIdentifier(currentLine, lines, tokens);
        if(ret != null) {
            isFirstCodeTokenInLine = false;

            return ret;
        }

        ret = tryTokenizeBracket(currentLine, lines, tokens);
        if(ret != null) {
            isFirstCodeTokenInLine = false;

            return ret;
        }

        ret = tryTokenizeOperator(currentLine, lines, tokens);
        if(ret != null) {
            isFirstCodeTokenInLine = false;

            return ret;
        }

        isFirstCodeTokenInLine = false;
        return null;
    }

    private String tryTokenizeNewLine(String currentLine, List<String> lines, List<Token> tokens) {
        if(currentLine.isEmpty() && !linesIsEmpty) {
            int fromColumn = column;
            column++;
            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "\n", Token.TokenType.EOL));

            lineNumber++;
            column = 1;

            openingBracketCount = 0;

            if(lines.isEmpty())
                linesIsEmpty = true;
            else
                currentLine = lines.remove(0);

            return currentLine;
        }

        return null;
    }

    private String tryTokenizeWhitespace(String currentLine, List<String> lines, List<Token> tokens) {
        int i = 0;
        char c;
        while(i < currentLine.length()) {
            c = currentLine.charAt(i);

            if(c != ' ' && c != '\t')
                break;

            i++;
        }

        if(i > 0) {
            int fromColumn = column;
            column += i;

            String token = currentLine.substring(0, i);
            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.WHITESPACE));

            return currentLine.substring(i);
        }

        return null;
    }

    private String tryTokenizeMultilineText(String currentLine, List<String> lines, List<Token> tokens) {
        if(currentLine.startsWith("\"\"\""))
            return tryTokenizeMultilineTextWithEscapeSequenceSupport(currentLine, lines, tokens);

        return tryTokenizeMultilineTextWithoutEscapeSequenceSupport(currentLine, lines, tokens);
    }

    private String tryTokenizeMultilineTextWithEscapeSequenceSupport(String currentLine, List<String> lines, List<Token> tokens) {
        if(!currentLine.startsWith("\"\"\""))
            return null;

        int fromColumn = column;
        column += 3;

        tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "\"\"\"", Token.TokenType.START_MULTILINE_TEXT));

        currentLine = currentLine.substring(3);

        while(true) {
            int endIndex = currentLine.indexOf("\"\"\"");
            if(endIndex == 0)
                break;

            int index = currentLine.indexOf("\\");
            if(index != -1 && (endIndex == -1?index < currentLine.length() - 1:index < endIndex)) {
                fromColumn = column;
                column += index;

                String token = currentLine.substring(0, index);
                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.LITERAL_TEXT));

                currentLine = currentLine.substring(index);

                String ret = tryTokenizeEscapeSequence(currentLine, lines, tokens);
                if(ret != null)
                    currentLine = ret;

                continue;
            }

            boolean wasLinesEmpty = lines.isEmpty();
            String ret = tryTokenizeNewLine(currentLine, lines, tokens);
            if(ret == null) {
                int len = endIndex == -1?currentLine.length():endIndex;

                fromColumn = column;
                column += len;

                String token = currentLine.substring(0, len);

                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.LITERAL_TEXT));

                currentLine = currentLine.substring(len);
            }else {
                if(wasLinesEmpty) {
                    tokens.add(new Token(lineNumber, lineNumber, column, column, "", Token.TokenType.END_MULTILINE_TEXT));
                    tokens.add(new Token(lineNumber, lineNumber, column, column,
                            "Multiline text closing bracket '\"\"\"' is missing!", Token.TokenType.LEXER_ERROR));

                    return "";
                }

                currentLine = ret;
            }
        }

        //Add empty LITERAL_TEXT token
        tokens.add(new Token(lineNumber, lineNumber, column, column, "", Token.TokenType.LITERAL_TEXT));

        fromColumn = column;
        column += 3;

        tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "\"\"\"", Token.TokenType.END_MULTILINE_TEXT));

        return currentLine.substring(3);
    }

    private String tryTokenizeMultilineTextWithoutEscapeSequenceSupport(String currentLine, List<String> lines, List<Token> tokens) {
        if(!currentLine.startsWith("{{{"))
            return null;

        int fromColumn = column;
        column += 3;

        tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "{{{", Token.TokenType.START_MULTILINE_TEXT));

        currentLine = currentLine.substring(3);

        while(!currentLine.contains("}}}")) {
            boolean wasLinesEmpty = lines.isEmpty();
            String ret = tryTokenizeNewLine(currentLine, lines, tokens);
            if(ret == null) {
                fromColumn = column;
                column += currentLine.length();

                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, currentLine, Token.TokenType.LITERAL_TEXT));

                currentLine = "";
            }else {
                if(wasLinesEmpty) {
                    tokens.add(new Token(lineNumber, lineNumber, column, column, "", Token.TokenType.END_MULTILINE_TEXT));
                    tokens.add(new Token(lineNumber, lineNumber, column, column,
                            "Multiline text closing bracket \"}}}\" is missing!", Token.TokenType.LEXER_ERROR));

                    return "";
                }

                currentLine = ret;
            }
        }

        int index = currentLine.indexOf("}}}");

        //Add LITERAL_TEXT node even if text is empty
        fromColumn = column;
        column += index;

        String token = currentLine.substring(0, index);
        tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.LITERAL_TEXT));

        currentLine = currentLine.substring(index);

        fromColumn = column;
        column += 3;

        tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "}}}", Token.TokenType.END_MULTILINE_TEXT));

        return currentLine.substring(3);
    }

    private String tryTokenizeSingleLineText(String currentLine, List<String> lines, List<Token> tokens) {
        if(!currentLine.startsWith("\""))
            return null;

        int endIndex = 1;
        if(endIndex == currentLine.length())
            return null;

        while(endIndex < currentLine.length()) {
            endIndex = currentLine.indexOf("\"", endIndex);

            if(endIndex == -1)
                return null;

            if(currentLine.charAt(endIndex - 1) != '\\' || LangUtils.isBackslashAtIndexEscaped(currentLine, endIndex - 1))
                break;

            endIndex++;
        }

        int fromColumn = column;
        column += 1;
        endIndex -= 1;

        tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "\"", Token.TokenType.SINGLE_LINE_TEXT_QUOTES));

        currentLine = currentLine.substring(1);

        if(endIndex == 0)
            tokens.add(new Token(lineNumber, lineNumber, column, column, "", Token.TokenType.LITERAL_TEXT));

        while(endIndex > 0) {
            int index = currentLine.indexOf("\\");

            if(index != -1 && index < endIndex) {
                fromColumn = column;
                column += index;
                endIndex -= index;

                String token = currentLine.substring(0, index);
                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.LITERAL_TEXT));

                currentLine = currentLine.substring(index);

                String ret = tryTokenizeEscapeSequence(currentLine, lines, tokens);
                if(ret != null) {
                    endIndex -= currentLine.length() - ret.length();
                    currentLine = ret;
                }

                continue;
            }

            fromColumn = column;
            column += endIndex;

            String token = currentLine.substring(0, endIndex);
            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.LITERAL_TEXT));

            currentLine = currentLine.substring(endIndex);

            break;
        }

        fromColumn = column;
        column += 1;

        tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "\"", Token.TokenType.SINGLE_LINE_TEXT_QUOTES));

        return currentLine.substring(1);
    }

    private String tryTokenizeLineContinuation(String currentLine, List<String> lines, List<Token> tokens) {
        if(currentLine.length() == 1 && currentLine.charAt(0) == '\\') {
            int originalOpenBracketCount = openingBracketCount;

            int fromColumn = column;
            column++;

            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "\\", Token.TokenType.LINE_CONTINUATION));

            currentLine = "";
            String ret = tryTokenizeNewLine(currentLine, lines, tokens);
            if(ret != null)
                currentLine = ret;

            openingBracketCount = originalOpenBracketCount;
            return currentLine;
        }

        return null;
    }

    private String tryTokenizeComment(String currentLine, List<String> lines, List<Token> tokens) {
        char c = currentLine.charAt(0);
        if(c != '#')
            return null;

        if(currentLine.length() > 1 && currentLine.charAt(1) == '#') {
            int fromColumn = column;
            column += 2;

            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "##", Token.TokenType.START_DOC_COMMENT));

            currentLine = currentLine.substring(2);
        }else {
            int fromColumn = column;
            column++;

            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, "#", Token.TokenType.START_COMMENT));

            currentLine = currentLine.substring(1);
        }

        while(!currentLine.isEmpty()) {
            int multilineTextStartIndex;
            {
                int multilineTextStartIndex1 = currentLine.indexOf("{{{");
                int multilineTextStartIndex2 = currentLine.indexOf("\"\"\"");

                if(multilineTextStartIndex1 == -1) {
                    multilineTextStartIndex = multilineTextStartIndex2;
                }else {
                    if(multilineTextStartIndex2 == -1) {
                        multilineTextStartIndex = multilineTextStartIndex1;
                    }else {
                        multilineTextStartIndex = Math.min(multilineTextStartIndex1, multilineTextStartIndex2);
                    }
                }
            }

            if(multilineTextStartIndex != -1) {
                if(multilineTextStartIndex > 0) {
                    int fromColumn = column;
                    column += multilineTextStartIndex;

                    String token = currentLine.substring(0, multilineTextStartIndex);
                    tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.LITERAL_TEXT));

                    currentLine = currentLine.substring(multilineTextStartIndex);
                }

                String ret = tryTokenizeMultilineText(currentLine, lines, tokens);
                if(ret != null)
                    currentLine = ret;

                continue;
            }

            if(currentLine.endsWith("\\")) {
                if(currentLine.length() > 1) {
                    int fromColumn = column;
                    column += currentLine.length() - 1;

                    String token = currentLine.substring(0, currentLine.length() - 1);
                    tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.LITERAL_TEXT));

                    currentLine = currentLine.substring(currentLine.length() - 1);
                }

                String ret = tryTokenizeLineContinuation(currentLine, lines, tokens);
                if(ret != null)
                    currentLine = ret;

                continue;
            }

            int fromColumn = column;
            column += currentLine.length();

            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, currentLine, Token.TokenType.LITERAL_TEXT));

            currentLine = "";
        }

        tokens.add(new Token(lineNumber, lineNumber, column, column, "", Token.TokenType.END_COMMENT));

        return currentLine;
    }

    private String tryTokenizeBracket(String currentLine, List<String> lines, List<Token> tokens) {
        char c = currentLine.charAt(0);

        if(c == '{' || c == '(' || c == '[') {
            boolean isLastCodeTokenInLine = c == '{';

            int lineIndex = 0;
            int i = 1;
            String line = isLastCodeTokenInLine?currentLine:"";
            while(i < line.length()) {
                c = line.charAt(i);

                if(c == '\\' && i == line.length() - 1 && lineIndex < lines.size()) {
                    line = lines.get(lineIndex);
                    lineIndex++;
                    i = 0;

                    continue;
                }

                if(c == '#') {
                    break;
                }

                if(c != ' ' && c != '\t') {
                    isLastCodeTokenInLine = false;

                    break;
                }

                i++;
            }

            if(c == '{' && isLastCodeTokenInLine)
                openingBlockCount++;
            else
                openingBracketCount++;

            int fromColumn = column;
            column++;

            String token = currentLine.substring(0, 1);
            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, isLastCodeTokenInLine?
                    Token.TokenType.OPENING_BLOCK_BRACKET:Token.TokenType.OPENING_BRACKET));

            return currentLine.substring(1);
        }else if(c == '}' || c == ')' || c == ']') {
            isFirstCodeTokenInLine &= c == '}';

            if(isFirstCodeTokenInLine) {
                openingBlockCount--;
                if(openingBlockCount < 0)
                    openingBlockCount = 0;
            }else {
                openingBracketCount--;
                if(openingBracketCount < 0)
                    openingBracketCount = 0;
            }

            int fromColumn = column;
            column++;

            String token = currentLine.substring(0, 1);
            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, isFirstCodeTokenInLine?
                    Token.TokenType.CLOSING_BLOCK_BRACKET:Token.TokenType.CLOSING_BRACKET));

            return currentLine.substring(1);
        }

        return null;
    }

    private String tryTokenizeEscapeSequence(String currentLine, List<String> lines, List<Token> tokens) {
        if(currentLine.length() >= 2 && currentLine.charAt(0) == '\\') {
            unicodeEscapeSequence:
            if(currentLine.charAt(1) == 'u' && currentLine.length() >= 5 && currentLine.charAt(2) == '{' &&
                    currentLine.charAt(3) != '}' && currentLine.contains("}")) {
                int i = 3;
                while(i < 10 && i < currentLine.length()) {
                    char c = currentLine.charAt(i);
                    if(c == '}')
                        break;

                    if(!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')))
                        break unicodeEscapeSequence;

                    i++;
                }

                if(currentLine.charAt(i) != '}')
                    break unicodeEscapeSequence;

                i++;

                int fromColumn = column;
                column += i;

                String token = currentLine.substring(0, i);
                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.ESCAPE_SEQUENCE));

                return currentLine.substring(i);
            }

            int fromColumn = column;
            column += 2;

            String token = currentLine.substring(0, 2);
            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.ESCAPE_SEQUENCE));

            return currentLine.substring(2);
        }

        return null;
    }

    private String tryTokenizeParserFunctionIdentifier(String currentLine, List<String> lines, List<Token> tokens) {
        Matcher matcher = LangPatterns.PARSER_FUNCTION_IDENTIFIER.matcher(currentLine);
        if(matcher.find()) {
            String token = matcher.group();
            int index = currentLine.indexOf(token);
            if(index == 0) {
                int fromColumn = column;
                column += token.length();

                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.PARSER_FUNCTION_IDENTIFIER));

                return currentLine.substring(token.length());
            }
        }

        return null;
    }

    private String tryTokenizeIdentifier(String currentLine, List<String> lines, List<Token> tokens) {
        Matcher matcher = LangPatterns.VAR_NAME_FULL_WITH_FUNCS_AND_PTR_AND_DEREFERENCE_WITH_OPERATOR_AND_CONVERSION_METHODS.matcher(currentLine);
        if(matcher.find()) {
            String token = matcher.group();
            int index = currentLine.indexOf(token);
            if(index == 0) {
                //Check if var pointer brackets are closed correctly
                if(LangPatterns.matches(token, LangPatterns.PARSING_VAR_NAME_PTR_AND_DEREFERENCE)) {
                    int endIndex = LangUtils.getIndexOfMatchingBracket(token, token.indexOf('$') + 1, Integer.MAX_VALUE, '[', ']');
                    if(endIndex == -1) {
                        tokens.add(new Token(lineNumber, lineNumber, column, column + token.length(),
                                "Bracket is missing in variable pointer: \"" + token + "\"",
                                Token.TokenType.LEXER_ERROR));

                        return "";
                    }

                    //Limit token to end with closing "]"
                    token = token.substring(0, endIndex + 1);
                }

                int fromColumn = column;
                column += token.length();

                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.IDENTIFIER));

                return currentLine.substring(token.length());
            }
        }

        return null;
    }

    private String tryTokenizeOperator(String currentLine, List<String> lines, List<Token> tokens) {
        for(String operator:OPERATORS) {
            if(currentLine.startsWith(operator)) {
                int fromColumn = column;
                column += operator.length();

                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, operator, Token.TokenType.OPERATOR));

                return currentLine.substring(operator.length());
            }
        }
        return null;
    }

    private String tryTokenizeArgumentSeparator(String currentLine, List<String> lines, List<Token> tokens) {
        Matcher matcher = LangPatterns.ARGUMENT_SEPARATOR.matcher(currentLine);
        if(matcher.find()) {
            String token = matcher.group();
            int index = currentLine.indexOf(token);
            if(index == 0) {
                int fromColumn = column;
                column += token.length();

                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.ARGUMENT_SEPARATOR));

                return currentLine.substring(token.length());
            }
        }

        return null;
    }

    private String tryTokenizeAssignment(String currentLine, List<String> lines, List<Token> tokens) {
        if(openingBracketCount > 0)
            return null;

        Matcher matcher = LangPatterns.PARSING_ASSIGNMENT_OPERATOR.matcher(currentLine);
        if(matcher.find()) {
            String token = matcher.group();
            int index = currentLine.indexOf(token);
            if(index == 0) {
                int fromColumn = column;
                column += token.length();

                tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.ASSIGNMENT));

                return currentLine.substring(token.length());
            }
        }

        if(currentLine.startsWith(" = ")) {
            int fromColumn = column;
            column += 3;

            String token = currentLine.substring(0, 3);
            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.ASSIGNMENT));

            return currentLine.substring(token.length());
        }else if(currentLine.equals(" =")) {
            int fromColumn = column;
            column += 2;

            String token = currentLine.substring(0, 2);
            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.ASSIGNMENT));

            return currentLine.substring(token.length());
        }else if(currentLine.startsWith("=")) {
            int fromColumn = column;
            column++;

            String token = currentLine.substring(0, 1);
            tokens.add(new Token(lineNumber, lineNumber, fromColumn, column, token, Token.TokenType.ASSIGNMENT));

            currentLine = currentLine.substring(token.length());

            return currentLine;
        }

        return null;
    }

    Token tokenizeOtherValue(String token, CodePosition pos) {
        Token.TokenType tokenType = Token.TokenType.OTHER;

        if(isNullValue(token))
            tokenType = Token.TokenType.LITERAL_NULL;
        else if(isNumericValue(token))
            tokenType = Token.TokenType.LITERAL_NUMBER;

        return new Token(pos, token, tokenType);
    }

    private boolean isNullValue(String token) {
        return token.equals("null");
    }

    private boolean isNumericValue(String token) {
        char c = token.isEmpty()?0:token.charAt(0);
        if(!(c >= '0' && c <= '9') && c != '.')
            return false;

        //INT
        try {
            Integer.parseInt(token);

            return true;
        }catch(NumberFormatException ignore) {}

        //LONG
        try {
            if(token.endsWith("l") || token.endsWith("L"))
                Long.parseLong(token.substring(0, token.length() - 1));
            else
                Long.parseLong(token);

            return true;
        }catch(NumberFormatException ignore) {}

        //FLOAT
        if(token.endsWith("f") || token.endsWith("F")) {
            //Do not allow: NaN, Infinity, xX
            if(token.contains("N") || token.contains("I") || token.contains("x") || token.contains("X"))
                return false;

            try {
                Float.parseFloat(token.substring(0, token.length() - 1));

                return true;
            }catch(NumberFormatException ignore) {}
        }

        //DOUBLE
        try {
            //Do not allow: NaN, Infinity, xX, dD
            if(token.endsWith("d") || token.endsWith("D") || token.contains("N") || token.contains("I") ||
                    token.contains("x") || token.contains("X"))
                return false;

            Double.parseDouble(token);

            return true;
        }catch(NumberFormatException ignore) {}

        return false;
    }
}
