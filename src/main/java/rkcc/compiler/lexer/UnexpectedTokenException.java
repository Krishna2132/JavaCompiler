package rkcc.compiler.lexer;

import rkcc.compiler.diagnostics.SourceLocation;

/**
 * Minimal unchecked exception thrown by {@link TokenStream#consume(TokenType, String)}
 * when the current token does not match the expected {@link TokenType}.
 *
 * <p>Preserves expected type, actual token, and {@link SourceLocation} metadata for
 * future parser error reporting and diagnostics integration.</p>
 */
public class UnexpectedTokenException extends RuntimeException {

    private final TokenType expectedType;
    private final Token actualToken;

    /**
     * Constructs an {@code UnexpectedTokenException} with structured token context.
     *
     * @param expectedType non-null expected token type
     * @param actualToken non-null actual token encountered
     * @param message non-blank contextual error message
     */
    public UnexpectedTokenException(
            TokenType expectedType,
            Token actualToken,
            String message) {
        super(formatMessage(expectedType, actualToken, message));
        this.expectedType = expectedType;
        this.actualToken = actualToken;
    }

    /**
     * Returns the {@link TokenType} expected by the caller.
     */
    public TokenType getExpectedType() {
        return expectedType;
    }

    /**
     * Returns the actual {@link Token} found at the current stream position.
     */
    public Token getActualToken() {
        return actualToken;
    }

    /**
     * Returns the {@link SourceLocation} of the unexpected token.
     */
    public SourceLocation getLocation() {
        return actualToken.location();
    }

    private static String formatMessage(
            TokenType expectedType,
            Token actualToken,
            String message) {
        if (expectedType == null) {
            throw new IllegalArgumentException("expectedType must not be null");
        }
        if (actualToken == null) {
            throw new IllegalArgumentException("actualToken must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank");
        }
        return message.trim()
                + " [expected=" + expectedType
                + ", actual=" + actualToken.type()
                + " ('" + actualToken.lexeme() + "')"
                + ", location=" + actualToken.location().formatCompact()
                + "]";
    }
}
