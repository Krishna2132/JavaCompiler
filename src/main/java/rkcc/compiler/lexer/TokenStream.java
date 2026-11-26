package rkcc.compiler.lexer;

/**
 * Minimal token-access contract consumed by parsers and {@code AbstractTypeParser}.
 *
 * <p>Decouples syntactic parsing from concrete lexer scanning internals.</p>
 */
public interface TokenStream {

    /**
     * Returns the current token without advancing the stream cursor.
     *
     * @return current token
     */
    Token peek();

    /**
     * Returns the token at {@code offset} positions ahead of the current cursor.
     *
     * @param offset non-negative lookahead offset ({@code 0} returns the current token)
     * @return token at the requested offset
     */
    Token peek(int offset);

    /**
     * Consumes and returns the current token, advancing the cursor by one position.
     *
     * @return the consumed token
     */
    Token advance();

    /**
     * Indicates whether the stream has reached the end of input.
     *
     * @return {@code true} if no further parsable tokens remain
     */
    boolean isAtEnd();

    /**
     * Checks whether the current token matches {@code expectedKind} and advances if so.
     *
     * @param expectedKind expected token kind identifier
     * @return {@code true} if matched and consumed, {@code false} otherwise
     */
    boolean match(String expectedKind);
}
