import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class Lexer {

    private Queue<Character> queue;
    private Lang lang;
    private String lexemeBuffer;
    private List<Token> tokens;

    public Lexer(String inputFile) {
        lang = new Lang();
        tokens = new ArrayList<>();

        // Read input file
        String inputStr = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            StringBuilder stringBuilder = new StringBuilder();
            char[] buffer = new char[16];
            while (reader.read(buffer) != -1) {
                stringBuilder.append(new String(buffer));
                buffer = new char[16];
            }
            reader.close();
            inputStr = stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create queue
        queue = new ArrayBlockingQueue<>(inputStr.length());
        char[] input = inputStr.toCharArray();
        for (char character : input) if (character != 0) queue.add(character);
    }

    public void printTokens() {
        for (Token token : tokens) {
            System.out.println(String.format("%s : %s", token.type, token.value));
        }
    }

    private void appendLexeme(char c) {
        lexemeBuffer = lexemeBuffer.concat(String.valueOf(c));
        queue.poll();
    }

    public void generateTokens() {
        while (!queue.isEmpty()) {
            lexemeBuffer = "";
            Token token = getNextToken();
            if (token == null) continue;
            if (token.value != null) token.value = token.value.trim();
            tokens.add(token);
        }
    }

    private Token getNextToken() {
        while (!queue.isEmpty()) {
            char c = queue.peek();
            appendLexeme(c);

            switch (c) {
                // Literal
                case '"':
                    return getLiteralToken();
                // Char
                case '\'':
                    return getCharToken();
                // Comment
                case '/':
                    if (queue.peek() == '/' || queue.peek() == '*') return getCommentToken();
                    break;
                case '#':
                    return getDirectiveToken();
            }

            // Operator
            if (lang.isOperatorChar(c)) return getOperatorToken();
            // Number
            if (lang.isDigit(c)) return getNumberToken();
            // Keyword or identifier
            if (lang.isKeywordChar(c)) return getIdOrKeywordToken(1);
            // Identifier
            if (lang.isIdStart(c)) return getIdOrKeywordToken(2);
            // Punctuation
            if (lang.isPunctuation(String.valueOf(c))) return getPunctuationToken(c);
        }

        return null;
    }

    private Token getLiteralToken() {
        if (lexemeBuffer.length() > 1) lexemeBuffer = lexemeBuffer.substring(lexemeBuffer.length() - 1, lexemeBuffer.length());
        char c;

        while (!queue.isEmpty()) {
            c = queue.peek();
            appendLexeme(c);
            if (c == '"') {
                // " escape
                if (lexemeBuffer.length() > 1 && lexemeBuffer.substring(lexemeBuffer.length() - 2, lexemeBuffer.length()).equals("\\\"")) {
                    lexemeBuffer = lexemeBuffer.substring(0, lexemeBuffer.length() - 2);
                    lexemeBuffer = lexemeBuffer.concat("\"");
                }
                // End of string
                else {
                    //lexemeBuffer = lexemeBuffer.substring(0, lexemeBuffer.length() - 1);
                    break;
                }
            }
        }

        return new Token(TokenType.LITERAL, lexemeBuffer);
    }

    private Token getCharToken() {
        if (lexemeBuffer.length() > 1) lexemeBuffer = lexemeBuffer.substring(lexemeBuffer.length() - 1, lexemeBuffer.length());
        char c;

        while (!queue.isEmpty()) {
            c = queue.peek();
            appendLexeme(c);
            if (c == '\'') {
                // ' escape
                if (lexemeBuffer.length() > 1 && lexemeBuffer.substring(lexemeBuffer.length() - 2, lexemeBuffer.length()).equals("\\\"")) {
                    lexemeBuffer = lexemeBuffer.substring(0, lexemeBuffer.length() - 2);
                    lexemeBuffer = lexemeBuffer.concat("\'");
                }
                // End of char
                else {
                    break;
                }
            }
        }

        // Check length
        if (lexemeBuffer.length() == 3 && lexemeBuffer.charAt(1) != '\\') return new Token(TokenType.CHAR, lexemeBuffer);
        if (lexemeBuffer.length() == 3 && lexemeBuffer.charAt(1) == '\\'
                || lexemeBuffer.length() < 3 || lexemeBuffer.length() > 4) return new Token(TokenType.ERROR, lexemeBuffer);
        if (lexemeBuffer.charAt(1) == '\\' && lang.isEscapeChar(lexemeBuffer.charAt(2))) return new Token(TokenType.CHAR, lexemeBuffer);
        return new Token(TokenType.ERROR, lexemeBuffer);
    }

    private Token getNumberToken() {
        if (lexemeBuffer.length() > 1) lexemeBuffer = lexemeBuffer.substring(lexemeBuffer.length() - 1, lexemeBuffer.length());

        int state = 1;
        while (!queue.isEmpty()) {
            int nextState = state;
            char c = queue.peek();
            appendLexeme(c);

            switch (state) {
                // 12, 56, etc
                case 1:
                    if (lang.isDigit(c)) nextState = 1;
                    else if (c == '.') nextState = 2;
                    else if (c == 'u' || c == 'U') nextState = 3;
                    else if (c == 'l' || c == 'L' || c == 'f' || c == 'F' || c == 'm' || c == 'M') nextState = 4;
                    else if (lang.canFollowNumber(c)) return new Token(TokenType.NUMBER, lexemeBuffer);
                    else return new Token(TokenType.ERROR, lexemeBuffer);
                    break;

                // 12., 56., etc.
                case 2:
                    if (lang.isDigit(c)) nextState = 5;
                    else return new Token(TokenType.ERROR, lexemeBuffer);
                    break;

                // 12U, 56u, etc.
                case 3:
                    if (c == 'l' || c == 'L') nextState = 4;
                    else if (lang.canFollowNumber(c)) return new Token(TokenType.NUMBER, lexemeBuffer);
                    else return new Token(TokenType.ERROR, lexemeBuffer);
                    break;

                // 12L, 56ul, 12.5f, etc.
                case 4:
                    if (lang.canFollowNumber(c)) return new Token(TokenType.NUMBER, lexemeBuffer);
                    else return new Token(TokenType.ERROR, lexemeBuffer);

                // 5.2, 8.5, etc.
                case 5:
                    if (lang.isDigit(c)) nextState = 5;
                    else if (c == 'f' || c == 'F' || c == 'm' || c == 'M') nextState = 4;
                    else if (lang.canFollowNumber(c)) return new Token(TokenType.NUMBER, lexemeBuffer);
                    else return new Token(TokenType.ERROR, lexemeBuffer);
                    break;
            }

            state = nextState;
        }

        return new Token(TokenType.NUMBER, lexemeBuffer);
    }

    private Token getCommentToken() {
        if (lexemeBuffer.length() > 1) lexemeBuffer = lexemeBuffer.substring(lexemeBuffer.length() - 1, lexemeBuffer.length());
        int state = 1;

        while (true) {
            int nextState = state;
            char c = queue.peek();

            switch (state) {
                case 1:
                    // "/" -> "//"
                    if (c == '/') nextState = 2;
                    // "/" -> "/*"
                    else if (c == '*') nextState = 3;
                    break;
                case 2:
                    // end of "//" comment
                    if (lang.isLineBreak(c)) nextState = 5;
                    // Comment content
                    else nextState = 2;
                    break;
                case 3:
                    // "/*..." -> "/*...*"
                    if (c == '*') nextState = 4;
                    // Comment content
                    else nextState = 3;
                    break;
                case 4:
                    // "/*...*" -> "/*...*/"
                    if (c == '/') nextState = 5;
                    // "/*...*"-> "/*...*." (* is the part of content)
                    else nextState = 3;
                    break;
                case 5:
                    // Comment end
                    return new Token(TokenType.COMMENT, lexemeBuffer);
            }

            appendLexeme(c);

            if (queue.peek() == null) return new Token(TokenType.COMMENT, lexemeBuffer);
            state = nextState;
        }
    }

    private Token getDirectiveToken() {
        if (lexemeBuffer.length() > 1) lexemeBuffer = lexemeBuffer.substring(lexemeBuffer.length() - 1, lexemeBuffer.length());

        while (!queue.isEmpty()) {
            char c = queue.peek();

            // Possible end of directive
            if (c == ' ' || lang.isLineBreak(c)) {
                // Directive
                if (lang.isDirective(lexemeBuffer) && !lexemeBuffer.contains("pragma")) return new Token(TokenType.DIRECTIVE, lexemeBuffer);
                // One of #pragma ...
                if (lexemeBuffer.contains("pragma")) {
                    // #pragma smth
                    if (lexemeBuffer.contains(" ") && lang.isDirective(lexemeBuffer)) return new Token(TokenType.DIRECTIVE, lexemeBuffer);
                    if (lexemeBuffer.contains(" ")) return new Token(TokenType.ERROR, lexemeBuffer);

                    // Line break, only "#pragma" possible
                    if (lang.isLineBreak(c) && lang.isDirective(lexemeBuffer)) return new Token(TokenType.DIRECTIVE, lexemeBuffer);
                    // #pragma ...(should consider one more word)
                    // Exit if and append lexeme
                }
            }

            appendLexeme(c);
        }
        return new Token(TokenType.DIRECTIVE, lexemeBuffer);
    }

    private Token getPunctuationToken(char character) {
        lexemeBuffer = "";
        return new Token(TokenType.PUNCTUATION, String.valueOf(character));
    }

    private Token getIdOrKeywordToken(int state) {
        if (lexemeBuffer.length() > 1) lexemeBuffer = lexemeBuffer.substring(lexemeBuffer.length() - 1, lexemeBuffer.length());

        while (true) {
            int nextState = state;
            char c = queue.peek();

            switch (state) {
                // Possible id or keyword
                case 1:
                    if (lang.isKeywordChar(c)) {
                        appendLexeme(c);
                        nextState = 1;
                    }
                    else if (lang.isIdChar(c)) {
                        appendLexeme(c);
                        nextState = 2;
                    }
                    else nextState = 3;
                    break;

                // Possible id
                case 2:
                    if (lang.isIdChar(c)) {
                        nextState = 2;
                        appendLexeme(c);
                    }
                    else {
                        nextState = 4;
                    }
                    break;

                // Possible id or keyword finished
                case 3:
                    if (lang.isKeyword(lexemeBuffer)) return new Token(TokenType.KEYWORD, lexemeBuffer);
                    else return new Token(TokenType.IDENTIFIER, lexemeBuffer);

                // Possible id finished
                case 4:
                    return new Token(TokenType.IDENTIFIER, lexemeBuffer);
            }

            if (queue.peek() == null) return new Token(TokenType.IDENTIFIER, lexemeBuffer);
            state = nextState;
        }
    }

    private Token getOperatorToken() {
        if (lexemeBuffer.length() > 1) lexemeBuffer = lexemeBuffer.substring(lexemeBuffer.length() - 1, lexemeBuffer.length());

        int state = 1;
        while (true) {
            char c = queue.peek();

            switch (state) {
                // Possible operator
                case 1:
                    if (lang.isOperatorChar(c)) appendLexeme(c);
                    else state = 2;
                    break;

                // Possible operator finished
                case 2:
                    if (lang.isOperator(lexemeBuffer)) return new Token(TokenType.OPERATOR, lexemeBuffer);
                    else return new Token(TokenType.ERROR, lexemeBuffer);
            }

            if (queue.peek() == null) return new Token(TokenType.ERROR, lexemeBuffer);
        }
    }
}
