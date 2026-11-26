package rkcc.compiler.lexer;

import rkcc.compiler.diagnostics.SourceLocation;

/**
 * Minimal unchecked exception thrown by {@link Lexer} when lexical scanning
 * encounters invalid characters, malformed literals, invalid escape sequences,
 * or unterminated strings, character literals, or block comments.
 *
 * <p>Preserves exact {@link SourceLocation} and offending lexeme metadata for
 * future compiler diagnostics integration.</p>
 */
public class LexicalException extends RuntimeException {

    private final SourceLocation location;
    private final String offendingLexeme;

    /**
     * Constructs a {@code LexicalException} without an offending lexeme snippet.
     *
     * @param message descriptive error message
     * @param location source location where the lexical error began
     */
    public LexicalException(String message, SourceLocation location) {
        this(message, location, "");
    }

    /**
     * Constructs a {@code LexicalException} with full source-location context.
     *
     * @param message descriptive error message
     * @param location non-null source location of the lexical error
     * @param offendingLexeme offending source fragment (non-null; may be empty)
     */
    public LexicalException(String message, SourceLocation location, String offendingLexeme) {
        super(formatMessage(message, location, offendingLexeme));
        this.location = location;
        this.offendingLexeme = offendingLexeme == null ? "" : offendingLexeme;
    }

    /**
     * Returns the source location associated with this lexical error.
     */
    public SourceLocation getLocation() {
        return location;
    }

    /**
     * Returns the offending lexeme fragment, or {@code ""} if not applicable.
     */
    public String getOffendingLexeme() {
        return offendingLexeme;
    }

    private static String formatMessage(
            String message,
            SourceLocation location,
            String offendingLexeme) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        StringBuilder sb = new StringBuilder(message.trim());
        if (offendingLexeme != null && !offendingLexeme.isEmpty()) {
            sb.append(" ('").append(offendingLexeme).append("')");
        }
        sb.append(" [at ").append(location.formatCompact())
                .append(", offset=").append(location.offset()).append(']');
        return sb.toString();
    }
}
