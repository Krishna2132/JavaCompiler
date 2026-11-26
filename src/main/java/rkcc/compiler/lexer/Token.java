package rkcc.compiler.lexer;

import rkcc.compiler.diagnostics.SourceLocation;

/**
 * Immutable lexical token pairing a strongly typed {@link TokenType}, the exact
 * original source lexeme, and a validated {@link SourceLocation}.
 *
 * <p>Numeric range validation and sign folding belong to semantic/literal
 * interpretation; unary minus is represented as {@link TokenType#MINUS} separately
 * from numeric literal tokens.</p>
 *
 * @param type non-null token type classification
 * @param lexeme non-null original source text (empty allowed for {@link TokenType#EOF})
 * @param location non-null starting source location of the token
 */
public record Token(TokenType type, String lexeme, SourceLocation location) {

    public Token {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (lexeme == null) {
            throw new IllegalArgumentException("lexeme must not be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        if (type != TokenType.EOF && lexeme.isEmpty()) {
            throw new IllegalArgumentException(
                    "Non-EOF token of type " + type + " must not have an empty lexeme");
        }
        if (type == TokenType.EOF && !lexeme.isEmpty()) {
            throw new IllegalArgumentException("EOF token must have an empty lexeme");
        }
    }

    /**
     * Creates an {@link TokenType#EOF} token with an empty lexeme at {@code location}.
     *
     * @param location non-null end-of-file source location
     * @return immutable EOF token
     */
    public static Token eof(SourceLocation location) {
        return new Token(TokenType.EOF, "", location);
    }

    /**
     * Checks whether this token has the specified {@link TokenType}.
     *
     * @param expectedType candidate token type
     * @return {@code true} if {@code this.type == expectedType}
     */
    public boolean is(TokenType expectedType) {
        return this.type == expectedType;
    }

    /**
     * Convenience check delegating to {@link TokenType#isPrimitiveTypeKeyword()}.
     */
    public boolean isPrimitiveTypeKeyword() {
        return type.isPrimitiveTypeKeyword();
    }

    @Override
    public String toString() {
        return "Token[type=" + type + ", lexeme='" + lexeme + "', location=" + location.formatCompact() + "]";
    }
}
