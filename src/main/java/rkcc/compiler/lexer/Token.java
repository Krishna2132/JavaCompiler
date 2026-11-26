package rkcc.compiler.lexer;

/**
 * Minimal immutable lexical token contract consumed through {@link TokenStream}.
 *
 * <p>Full token kinds and source location spans will be introduced with the lexer milestone.</p>
 */
public interface Token {

    /**
     * Returns the token category identifier (for example, {@code "KEYWORD"},
     * {@code "IDENTIFIER"}, or {@code "INT_LITERAL"}).
     *
     * @return non-null token kind name
     */
    String kind();

    /**
     * Returns the exact source text matched by this token.
     *
     * @return non-null token lexeme
     */
    String lexeme();
}
