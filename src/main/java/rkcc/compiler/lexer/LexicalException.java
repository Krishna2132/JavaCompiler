package rkcc.compiler.lexer;

import rkcc.compiler.diagnostics.CompilerException;
import rkcc.compiler.diagnostics.CompilerPhase;
import rkcc.compiler.diagnostics.Diagnostic;
import rkcc.compiler.diagnostics.DiagnosticCode;
import rkcc.compiler.diagnostics.DiagnosticSeverity;
import rkcc.compiler.diagnostics.SourceLocation;

/**
 * Unchecked exception thrown by {@link Lexer} when lexical scanning encounters
 * invalid characters, malformed literals, invalid escape sequences, or
 * unterminated strings, character literals, or block comments.
 *
 * <p>Extends {@link CompilerException} so every lexical failure carries a
 * structured {@link Diagnostic} with {@link DiagnosticCode},
 * {@link CompilerPhase#LEXICAL_ANALYSIS}, and exact {@link SourceLocation}.</p>
 */
public class LexicalException extends CompilerException {

    private final SourceLocation location;
    private final String offendingLexeme;

    /**
     * Constructs a {@code LexicalException} using {@link DiagnosticCode#LEX_UNEXPECTED_CHARACTER}.
     */
    public LexicalException(String message, SourceLocation location) {
        this(DiagnosticCode.LEX_UNEXPECTED_CHARACTER, message, location, "", null);
    }

    /**
     * Constructs a {@code LexicalException} with an offending lexeme fragment.
     */
    public LexicalException(String message, SourceLocation location, String offendingLexeme) {
        this(DiagnosticCode.LEX_UNEXPECTED_CHARACTER, message, location, offendingLexeme, null);
    }

    /**
     * Constructs a {@code LexicalException} with an explicit {@link DiagnosticCode}.
     */
    public LexicalException(
            DiagnosticCode code,
            String message,
            SourceLocation location,
            String offendingLexeme) {
        this(code, message, location, offendingLexeme, null);
    }

    /**
     * Constructs a {@code LexicalException} with an explicit {@link DiagnosticCode} and cause.
     */
    public LexicalException(
            DiagnosticCode code,
            String message,
            SourceLocation location,
            String offendingLexeme,
            Throwable cause) {
        super(buildDiagnostic(code, message, location), cause);
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

    private static Diagnostic buildDiagnostic(
            DiagnosticCode code,
            String message,
            SourceLocation location) {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        return new Diagnostic(
                code,
                DiagnosticSeverity.ERROR,
                CompilerPhase.LEXICAL_ANALYSIS,
                message.trim(),
                location);
    }
}
