package rkcc.compiler.lexer;

/**
 * Token-access contract consumed by parsers and {@code AbstractTypeParser}.
 *
 * <p>Decouples syntactic parsing from concrete lexer scanning internals while
 * providing controlled sequential consumption, lookahead, matching, and
 * expected-token verification over strongly typed {@link Token} instances.</p>
 */
public interface TokenStream {

    /**
     * Returns the current token without advancing the stream cursor.
     *
     * @return current token (or the terminal {@link TokenType#EOF} token when at end)
     */
    Token peek();

    /**
     * Returns the token at {@code offset} positions ahead of the current cursor
     * without modifying the cursor.
     *
     * @param offset non-negative lookahead offset ({@code 0} returns the current token)
     * @return token at the requested offset (clamped to {@link TokenType#EOF} past the end)
     * @throws IllegalArgumentException if {@code offset < 0}
     */
    Token peek(int offset);

    /**
     * Consumes and returns the current token, advancing the cursor by one position.
     *
     * <p>When already at {@link TokenType#EOF}, implementations must remain safely at EOF
     * and return the EOF token without advancing past the end.</p>
     *
     * @return the consumed token (or the terminal EOF token if already at end)
     */
    Token advance();

    /**
     * Returns the most recently consumed token.
     *
     * @return previously consumed token
     * @throws IllegalStateException if called before any token has been consumed
     */
    Token previous();

    /**
     * Checks whether the current token has {@code type} without consuming it.
     *
     * @param type expected token type
     * @return {@code true} if the current token matches {@code type}
     */
    boolean check(TokenType type);

    /**
     * Checks whether the current token matches {@code expectedType} and consumes it if so.
     *
     * @param expectedType expected token type
     * @return {@code true} if matched and consumed, {@code false} otherwise
     */
    boolean match(TokenType expectedType);

    /**
     * Consumes the current token if its type matches any of {@code types}.
     *
     * @param types candidate token types
     * @return {@code true} if one of {@code types} matched and was consumed, {@code false} otherwise
     */
    boolean match(TokenType... types);

    /**
     * Consumes the current token if it has {@code expectedType}, or throws an error
     * containing the expected type, actual token type, and source location.
     *
     * @param expectedType required token type
     * @param message descriptive diagnostic message on mismatch
     * @return the consumed matching token
     * @throws UnexpectedTokenException if the current token does not match {@code expectedType}
     */
    Token consume(TokenType expectedType, String message);

    /**
     * Indicates whether the stream cursor has reached {@link TokenType#EOF}.
     *
     * @return {@code true} if the current token is {@link TokenType#EOF}
     */
    boolean isAtEnd();

    /**
     * Returns the current 0-based cursor index in the token sequence.
     *
     * @return non-negative cursor position
     */
    int position();
}
