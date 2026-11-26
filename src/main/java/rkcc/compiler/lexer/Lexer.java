package rkcc.compiler.lexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rkcc.compiler.diagnostics.DiagnosticCode;
import rkcc.compiler.diagnostics.DiagnosticCollector;
import rkcc.compiler.diagnostics.SourceLocation;

/**
 * Lexical analyzer converting Java source code into immutable {@link Token} sequences
 * compatible with {@link ListTokenStream}.
 *
 * <h2>Source Location &amp; Unicode Conventions</h2>
 * <ul>
 *   <li><strong>Line:</strong> 1-based line counter incremented on {@code \r\n}, {@code \n}, or {@code \r}.</li>
 *   <li><strong>Column:</strong> 1-based character-unit column counter reset to {@code 1} after newlines;
 *       each tab ({@code \t}) advances column by {@code 1}.</li>
 *   <li><strong>Offset:</strong> 0-based UTF-16 code-unit index into the source {@code String}.</li>
 *   <li><strong>Identifiers:</strong> Evaluated using Unicode code points via
 *       {@link Character#isJavaIdentifierStart(int)} and {@link Character#isJavaIdentifierPart(int)}
 *       (excluding identifier-ignorable control characters).</li>
 * </ul>
 *
 * <h2>Supported Lexical Subset &amp; Limitations</h2>
 * <ul>
 *   <li>Supports primitive type keywords ({@code byte}..{@code void}), {@code true}, {@code false},
 *       {@code null}, identifiers ({@code String} is tokenized as {@link TokenType#IDENTIFIER}),
 *       decimal/binary/octal/hex integer literals, decimal floating-point literals with scientific
 *       notation, string and character literals with escapes, operators via longest-match,
 *       delimiters, whitespace, and comments ({@code //}, {@code /* * /}, {@code /** * /}).</li>
 *   <li>Hexadecimal floating-point literals (e.g., {@code 0x1.0p2}) and text blocks ({@code """})
 *       are rejected explicitly rather than silently mis-tokenized.</li>
 *   <li>Numeric range/overflow checks and unary minus folding are outside the lexer's scope;
 *       {@code -42} is emitted as {@link TokenType#MINUS} followed by {@link TokenType#INTEGER_LITERAL}.</li>
 * </ul>
 */
public final class Lexer {

    /**
     * Tokenizes the supplied Java source text into an unmodifiable list of {@link Token}s
     * terminated by a single {@link TokenType#EOF} token.
     *
     * @param source non-null Java source code string
     * @return unmodifiable list of tokens ending with {@link TokenType#EOF}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @throws LexicalException if invalid lexical input is encountered
     */
    public List<Token> tokenize(String source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        return new Scanner(source).scanAll();
    }

    /**
     * Tokenizes the supplied Java source text and records any fail-fast lexical
     * diagnostic into {@code diagnostics} before rethrowing {@link LexicalException}.
     *
     * @param source non-null Java source code string
     * @param diagnostics non-null diagnostic collector
     * @return unmodifiable list of tokens ending with {@link TokenType#EOF}
     * @throws IllegalArgumentException if {@code source} or {@code diagnostics} is {@code null}
     * @throws LexicalException if invalid lexical input is encountered
     */
    public List<Token> tokenize(String source, DiagnosticCollector diagnostics) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("diagnostics must not be null");
        }
        try {
            return tokenize(source);
        } catch (LexicalException ex) {
            diagnostics.report(ex.getDiagnostic());
            throw ex;
        }
    }

    /**
     * Tokenizes the supplied Java source text directly into a validated {@link TokenStream}.
     *
     * @param source non-null Java source code string
     * @return {@link ListTokenStream} wrapping the lexed tokens
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @throws LexicalException if invalid lexical input is encountered
     */
    public TokenStream tokenizeToStream(String source) {
        return new ListTokenStream(tokenize(source));
    }

    /**
     * Tokenizes the supplied Java source text into a {@link TokenStream} and records any
     * fail-fast lexical diagnostic into {@code diagnostics} before rethrowing.
     *
     * @param source non-null Java source code string
     * @param diagnostics non-null diagnostic collector
     * @return {@link ListTokenStream} wrapping the lexed tokens
     * @throws IllegalArgumentException if {@code source} or {@code diagnostics} is {@code null}
     * @throws LexicalException if invalid lexical input is encountered
     */
    public TokenStream tokenizeToStream(String source, DiagnosticCollector diagnostics) {
        return new ListTokenStream(tokenize(source, diagnostics));
    }

    private static final class Scanner {
        private final String source;
        private final int length;
        private final List<Token> tokens = new ArrayList<>();

        private int offset = 0;
        private int line = 1;
        private int column = 1;

        private Scanner(String source) {
            this.source = source;
            this.length = source.length();
        }

        private List<Token> scanAll() {
            while (!isAtEnd()) {
                skipWhitespaceAndComments();
                if (isAtEnd()) {
                    break;
                }
                tokens.add(scanNextToken());
            }
            tokens.add(Token.eof(currentLocation()));
            return Collections.unmodifiableList(tokens);
        }

        private void skipWhitespaceAndComments() {
            while (!isAtEnd()) {
                char ch = peek();
                if (isJavaWhitespace(ch)) {
                    advanceChar();
                } else if (ch == '/' && peek(1) == '/') {
                    skipLineComment();
                } else if (ch == '/' && peek(1) == '*') {
                    skipBlockComment();
                } else {
                    break;
                }
            }
        }

        private void skipLineComment() {
            advanceChar(); // '/'
            advanceChar(); // '/'
            while (!isAtEnd() && peek() != '\n' && peek() != '\r') {
                advanceChar();
            }
        }

        private void skipBlockComment() {
            SourceLocation start = currentLocation();
            advanceChar(); // '/'
            advanceChar(); // '*'
            while (!isAtEnd()) {
                if (peek() == '*' && peek(1) == '/') {
                    advanceChar(); // '*'
                    advanceChar(); // '/'
                    return;
                }
                advanceChar();
            }
            throw new LexicalException(
                    DiagnosticCode.LEX_UNTERMINATED_BLOCK_COMMENT,
                    "Unterminated block comment",
                    start,
                    "/*");
        }

        private Token scanNextToken() {
            SourceLocation start = currentLocation();
            int codePoint = source.codePointAt(offset);

            if (codePoint == '"') {
                return scanStringLiteral(start);
            }
            if (codePoint == '\'') {
                return scanCharLiteral(start);
            }
            if (isAsciiDigit(codePoint) || (codePoint == '.' && isAsciiDigit(peek(1)))) {
                return scanNumberLiteral(start);
            }
            if (Character.isJavaIdentifierStart(codePoint) && !Character.isIdentifierIgnorable(codePoint)) {
                return scanIdentifierOrKeyword(start);
            }
            return scanOperatorOrDelimiter(start);
        }

        private Token scanIdentifierOrKeyword(SourceLocation start) {
            int startOffset = offset;
            while (!isAtEnd()) {
                int cp = source.codePointAt(offset);
                if (Character.isJavaIdentifierPart(cp) && !Character.isIdentifierIgnorable(cp)) {
                    advanceCodePoint(cp);
                } else {
                    break;
                }
            }
            String lexeme = source.substring(startOffset, offset);
            TokenType type = TokenType.fromKeyword(lexeme).orElse(TokenType.IDENTIFIER);
            return new Token(type, lexeme, start);
        }

        private Token scanStringLiteral(SourceLocation start) {
            advanceChar(); // opening '"'
            if (peek() == '"' && peek(1) == '"') {
                throw new LexicalException(
                        DiagnosticCode.LEX_UNTERMINATED_STRING,
                        "Unsupported Java text block literal",
                        start,
                        "\"\"\"");
            }
            while (!isAtEnd()) {
                char ch = peek();
                if (ch == '"') {
                    advanceChar();
                    return new Token(TokenType.STRING_LITERAL, source.substring(start.offset(), offset), start);
                }
                if (ch == '\n' || ch == '\r') {
                    throw new LexicalException(
                            DiagnosticCode.LEX_UNTERMINATED_STRING,
                            "Unterminated string literal",
                            start,
                            source.substring(start.offset(), offset));
                }
                if (ch == '\\') {
                    consumeEscapeSequence(false);
                } else {
                    advanceChar();
                }
            }
            throw new LexicalException(
                    DiagnosticCode.LEX_UNTERMINATED_STRING,
                    "Unterminated string literal",
                    start,
                    source.substring(start.offset(), offset));
        }

        private Token scanCharLiteral(SourceLocation start) {
            advanceChar(); // opening '\''
            if (isAtEnd() || peek() == '\n' || peek() == '\r') {
                throw new LexicalException(
                        DiagnosticCode.LEX_INVALID_CHARACTER_LITERAL,
                        "Unterminated character literal",
                        start,
                        "'");
            }
            if (peek() == '\'') {
                advanceChar();
                throw new LexicalException(
                        DiagnosticCode.LEX_INVALID_CHARACTER_LITERAL,
                        "Empty character literal",
                        start,
                        "''");
            }

            if (peek() == '\\') {
                consumeEscapeSequence(true);
            } else {
                advanceCodePoint(source.codePointAt(offset));
            }

            if (isAtEnd() || peek() == '\n' || peek() == '\r') {
                throw new LexicalException(
                        DiagnosticCode.LEX_INVALID_CHARACTER_LITERAL,
                        "Unterminated character literal",
                        start,
                        source.substring(start.offset(), offset));
            }
            if (peek() != '\'') {
                while (!isAtEnd() && peek() != '\'' && peek() != '\n' && peek() != '\r') {
                    advanceChar();
                }
                if (peek() == '\'') {
                    advanceChar();
                }
                throw new LexicalException(
                        DiagnosticCode.LEX_INVALID_CHARACTER_LITERAL,
                        "Invalid character literal: expected exactly one character",
                        start,
                        source.substring(start.offset(), offset));
            }

            advanceChar(); // closing '\''
            return new Token(TokenType.CHARACTER_LITERAL, source.substring(start.offset(), offset), start);
        }

        private void consumeEscapeSequence(boolean inCharLiteral) {
            SourceLocation escapeLoc = currentLocation();
            advanceChar(); // '\\'
            if (isAtEnd() || peek() == '\n' || peek() == '\r') {
                throw new LexicalException(
                        inCharLiteral
                                ? DiagnosticCode.LEX_INVALID_CHARACTER_LITERAL
                                : DiagnosticCode.LEX_UNTERMINATED_STRING,
                        inCharLiteral
                                ? "Unterminated character literal after escape"
                                : "Unterminated string literal after escape",
                        escapeLoc,
                        "\\");
            }

            char esc = peek();
            switch (esc) {
                case 'b', 't', 'n', 'f', 'r', '"', '\'', '\\', 's' -> advanceChar();
                case 'u' -> {
                    advanceChar();
                    while (peek() == 'u') {
                        advanceChar();
                    }
                    for (int i = 0; i < 4; i++) {
                        if (!isHexDigit(peek())) {
                            throw new LexicalException(
                                    DiagnosticCode.LEX_INVALID_ESCAPE_SEQUENCE,
                                    "Invalid Unicode escape sequence",
                                    escapeLoc,
                                    source.substring(escapeLoc.offset(), offset));
                        }
                        advanceChar();
                    }
                }
                default -> {
                    if (isOctalDigit(esc)) {
                        int first = esc - '0';
                        advanceChar();
                        if (isOctalDigit(peek())) {
                            advanceChar();
                            if (first <= 3 && isOctalDigit(peek())) {
                                advanceChar();
                            }
                        }
                    } else {
                        throw new LexicalException(
                                DiagnosticCode.LEX_INVALID_ESCAPE_SEQUENCE,
                                "Invalid escape sequence '\\" + esc + "'",
                                escapeLoc,
                                "\\" + esc);
                    }
                }
            }
        }

        private Token scanNumberLiteral(SourceLocation start) {
            int startOffset = offset;

            if (peek() == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
                return scanPrefixedIntegerLiteral(start, 16, "hexadecimal");
            }
            if (peek() == '0' && (peek(1) == 'b' || peek(1) == 'B')) {
                return scanPrefixedIntegerLiteral(start, 2, "binary");
            }

            boolean startsWithLeadingDot = (peek() == '.');
            if (!startsWithLeadingDot) {
                scanDigits(10, start, "decimal integer");
            }

            boolean isFloat = false;
            if (peek() == '.') {
                isFloat = true;
                advanceChar(); // '.'
                if (peek() == '_') {
                    throw new LexicalException(
                            DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                            "Malformed floating-point literal: underscore cannot follow '.'",
                            start,
                            source.substring(startOffset, offset + 1));
                }
                if (isAsciiDigit(peek())) {
                    scanDigits(10, start, "floating-point fraction");
                } else if (startsWithLeadingDot) {
                    throw new LexicalException(
                            DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                            "Malformed floating-point literal",
                            start,
                            source.substring(startOffset, offset));
                }
                if (peek() == '.') {
                    throw new LexicalException(
                            DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                            "Malformed floating-point literal: multiple decimal points",
                            start,
                            source.substring(startOffset, offset + 1));
                }
            }

            if (peek() == 'e' || peek() == 'E') {
                isFloat = true;
                advanceChar();
                if (peek() == '+' || peek() == '-') {
                    advanceChar();
                }
                if (!isAsciiDigit(peek())) {
                    throw new LexicalException(
                            DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                            "Malformed floating-point literal: exponent has no digits",
                            start,
                            source.substring(startOffset, offset));
                }
                scanDigits(10, start, "floating-point exponent");
            }

            char suffix = peek();
            if (suffix == 'f' || suffix == 'F' || suffix == 'd' || suffix == 'D') {
                isFloat = true;
                advanceChar();
                ensureNoTrailingIdentifierPart(start, startOffset, "floating-point");
                return new Token(
                        TokenType.FLOATING_POINT_LITERAL,
                        source.substring(startOffset, offset),
                        start);
            }

            if (isFloat) {
                ensureNoTrailingIdentifierPart(start, startOffset, "floating-point");
                return new Token(
                        TokenType.FLOATING_POINT_LITERAL,
                        source.substring(startOffset, offset),
                        start);
            }

            if (suffix == 'l' || suffix == 'L') {
                advanceChar();
            }

            String digitsOnly = stripSuffix(source.substring(startOffset, offset));
            if (digitsOnly.length() > 1 && digitsOnly.charAt(0) == '0') {
                validateOctalDigits(digitsOnly, start, source.substring(startOffset, offset));
            }
            ensureNoTrailingIdentifierPart(start, startOffset, "integer");
            return new Token(
                    TokenType.INTEGER_LITERAL,
                    source.substring(startOffset, offset),
                    start);
        }

        private Token scanPrefixedIntegerLiteral(SourceLocation start, int radix, String label) {
            int startOffset = offset;
            advanceChar(); // '0'
            advanceChar(); // 'x'/'X' or 'b'/'B'

            if (!isDigitForRadix(peek(), radix)) {
                throw new LexicalException(
                        DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                        "Malformed " + label + " integer literal: expected digits after prefix",
                        start,
                        source.substring(startOffset, offset));
            }

            scanDigits(radix, start, label + " integer");

            if (radix == 16 && (peek() == '.' || peek() == 'p' || peek() == 'P')) {
                throw new LexicalException(
                        DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                        "Unsupported hexadecimal floating-point literal",
                        start,
                        source.substring(startOffset, offset + 1));
            }

            if (peek() == 'l' || peek() == 'L') {
                advanceChar();
            }
            ensureNoTrailingIdentifierPart(start, startOffset, label + " integer");
            return new Token(
                    TokenType.INTEGER_LITERAL,
                    source.substring(startOffset, offset),
                    start);
        }

        private void scanDigits(int radix, SourceLocation start, String contextLabel) {
            int segmentStart = offset;
            boolean lastWasUnderscore = false;
            while (!isAtEnd()) {
                char ch = peek();
                if (isDigitForRadix(ch, radix)) {
                    lastWasUnderscore = false;
                    advanceChar();
                } else if (ch == '_') {
                    if (offset == segmentStart) {
                        throw new LexicalException(
                                DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                                "Malformed " + contextLabel + " literal: leading underscore",
                                start,
                                source.substring(start.offset(), offset + 1));
                    }
                    lastWasUnderscore = true;
                    advanceChar();
                } else {
                    break;
                }
            }
            if (lastWasUnderscore) {
                throw new LexicalException(
                        DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                        "Malformed " + contextLabel + " literal: trailing underscore",
                        start,
                        source.substring(start.offset(), offset));
            }
        }

        private void validateOctalDigits(String digits, SourceLocation start, String fullLexeme) {
            for (int i = 0; i < digits.length(); i++) {
                char ch = digits.charAt(i);
                if (ch != '_' && !isOctalDigit(ch)) {
                    throw new LexicalException(
                            DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                            "Malformed octal integer literal: invalid digit '" + ch + "'",
                            start,
                            fullLexeme);
                }
            }
        }

        private void ensureNoTrailingIdentifierPart(
                SourceLocation start,
                int startOffset,
                String literalLabel) {
            if (!isAtEnd()) {
                int cp = source.codePointAt(offset);
                if ((Character.isJavaIdentifierPart(cp) && !Character.isIdentifierIgnorable(cp))
                        || cp == '.') {
                    advanceCodePoint(cp);
                    throw new LexicalException(
                            DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL,
                            "Malformed " + literalLabel + " literal: unexpected suffix/character",
                            start,
                            source.substring(startOffset, offset));
                }
            }
        }

        private Token scanOperatorOrDelimiter(SourceLocation start) {
            char ch = peek();
            TokenType matched = switch (ch) {
                case '(' -> singleChar(TokenType.LEFT_PAREN);
                case ')' -> singleChar(TokenType.RIGHT_PAREN);
                case '{' -> singleChar(TokenType.LEFT_BRACE);
                case '}' -> singleChar(TokenType.RIGHT_BRACE);
                case '[' -> singleChar(TokenType.LEFT_BRACKET);
                case ']' -> singleChar(TokenType.RIGHT_BRACKET);
                case ';' -> singleChar(TokenType.SEMICOLON);
                case ',' -> singleChar(TokenType.COMMA);
                case '.' -> singleChar(TokenType.DOT);
                case ':' -> singleChar(TokenType.COLON);
                case '?' -> singleChar(TokenType.QUESTION_MARK);
                case '~' -> singleChar(TokenType.BITWISE_COMPLEMENT);
                case '+' -> matchOperator('+', TokenType.INCREMENT, '=', TokenType.PLUS_ASSIGN, TokenType.PLUS);
                case '-' -> matchOperator('-', TokenType.DECREMENT, '=', TokenType.MINUS_ASSIGN, TokenType.MINUS);
                case '*' -> matchEqualOrSingle(TokenType.STAR_ASSIGN, TokenType.STAR);
                case '/' -> matchEqualOrSingle(TokenType.SLASH_ASSIGN, TokenType.SLASH);
                case '%' -> matchEqualOrSingle(TokenType.PERCENT_ASSIGN, TokenType.PERCENT);
                case '=' -> matchEqualOrSingle(TokenType.EQUAL_EQUAL, TokenType.ASSIGN);
                case '!' -> matchEqualOrSingle(TokenType.NOT_EQUAL, TokenType.LOGICAL_NOT);
                case '&' -> matchOperator('&', TokenType.LOGICAL_AND, '=', TokenType.BITWISE_AND_ASSIGN, TokenType.BITWISE_AND);
                case '|' -> matchOperator('|', TokenType.LOGICAL_OR, '=', TokenType.BITWISE_OR_ASSIGN, TokenType.BITWISE_OR);
                case '^' -> matchEqualOrSingle(TokenType.BITWISE_XOR_ASSIGN, TokenType.BITWISE_XOR);
                case '<' -> matchLeftAngle();
                case '>' -> matchRightAngle();
                default -> {
                    int cp = source.codePointAt(offset);
                    String bad = new String(Character.toChars(cp));
                    throw new LexicalException(
                            DiagnosticCode.LEX_UNEXPECTED_CHARACTER,
                            "Unexpected character '" + bad + "'",
                            start,
                            bad);
                }
            };
            return new Token(matched, source.substring(start.offset(), offset), start);
        }

        private TokenType singleChar(TokenType type) {
            advanceChar();
            return type;
        }

        private TokenType matchEqualOrSingle(TokenType withEqual, TokenType single) {
            advanceChar();
            if (peek() == '=') {
                advanceChar();
                return withEqual;
            }
            return single;
        }

        private TokenType matchOperator(
                char doubledChar,
                TokenType doubledType,
                char assignChar,
                TokenType assignType,
                TokenType singleType) {
            advanceChar();
            if (peek() == doubledChar) {
                advanceChar();
                return doubledType;
            }
            if (peek() == assignChar) {
                advanceChar();
                return assignType;
            }
            return singleType;
        }

        private TokenType matchLeftAngle() {
            advanceChar(); // '<'
            if (peek() == '<') {
                advanceChar();
                if (peek() == '=') {
                    advanceChar();
                    return TokenType.LEFT_SHIFT_ASSIGN;
                }
                return TokenType.LEFT_SHIFT;
            }
            if (peek() == '=') {
                advanceChar();
                return TokenType.LESS_THAN_OR_EQUAL;
            }
            return TokenType.LESS_THAN;
        }

        private TokenType matchRightAngle() {
            advanceChar(); // '>'
            if (peek() == '>') {
                advanceChar();
                if (peek() == '>') {
                    advanceChar();
                    if (peek() == '=') {
                        advanceChar();
                        return TokenType.UNSIGNED_RIGHT_SHIFT_ASSIGN;
                    }
                    return TokenType.UNSIGNED_RIGHT_SHIFT;
                }
                if (peek() == '=') {
                    advanceChar();
                    return TokenType.RIGHT_SHIFT_ASSIGN;
                }
                return TokenType.RIGHT_SHIFT;
            }
            if (peek() == '=') {
                advanceChar();
                return TokenType.GREATER_THAN_OR_EQUAL;
            }
            return TokenType.GREATER_THAN;
        }

        private char peek() {
            return peek(0);
        }

        private char peek(int lookahead) {
            int target = offset + lookahead;
            if (target < 0 || target >= length) {
                return '\0';
            }
            return source.charAt(target);
        }

        private void advanceChar() {
            char ch = source.charAt(offset++);
            if (ch == '\r') {
                if (!isAtEnd() && source.charAt(offset) == '\n') {
                    offset++;
                }
                line++;
                column = 1;
            } else if (ch == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }

        private void advanceCodePoint(int codePoint) {
            int charCount = Character.charCount(codePoint);
            if (charCount == 1) {
                advanceChar();
            } else {
                offset += charCount;
                column++;
            }
        }

        private boolean isAtEnd() {
            return offset >= length;
        }

        private SourceLocation currentLocation() {
            return new SourceLocation(line, column, offset);
        }

        private static String stripSuffix(String lexeme) {
            char last = lexeme.charAt(lexeme.length() - 1);
            if (last == 'l' || last == 'L') {
                return lexeme.substring(0, lexeme.length() - 1);
            }
            return lexeme;
        }

        private static boolean isJavaWhitespace(char ch) {
            return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f';
        }

        private static boolean isAsciiDigit(int ch) {
            return ch >= '0' && ch <= '9';
        }

        private static boolean isOctalDigit(char ch) {
            return ch >= '0' && ch <= '7';
        }

        private static boolean isHexDigit(char ch) {
            return (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
        }

        private static boolean isDigitForRadix(char ch, int radix) {
            return switch (radix) {
                case 2 -> ch == '0' || ch == '1';
                case 8 -> isOctalDigit(ch);
                case 10 -> isAsciiDigit(ch);
                case 16 -> isHexDigit(ch);
                default -> false;
            };
        }
    }
}
