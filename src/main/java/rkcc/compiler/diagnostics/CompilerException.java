package rkcc.compiler.diagnostics;

import java.util.Optional;

/**
 * Base runtime exception for source-code and compilation failures carrying a
 * structured {@link Diagnostic} and optional underlying cause.
 *
 * <p>Unrelated programming defects (for example, {@link IllegalArgumentException}
 * or {@link NullPointerException} on invalid API calls) should remain standard
 * Java exceptions rather than being masked as source diagnostics.</p>
 */
public class CompilerException extends RuntimeException {

    private final Diagnostic diagnostic;

    /**
     * Creates a {@code CompilerException} wrapping {@code diagnostic}.
     *
     * @param diagnostic non-null structured diagnostic
     */
    public CompilerException(Diagnostic diagnostic) {
        super(formatExceptionMessage(diagnostic));
        this.diagnostic = diagnostic;
    }

    /**
     * Creates a {@code CompilerException} wrapping {@code diagnostic} and {@code cause}.
     *
     * @param diagnostic non-null structured diagnostic
     * @param cause optional underlying cause
     */
    public CompilerException(Diagnostic diagnostic, Throwable cause) {
        super(formatExceptionMessage(diagnostic), cause);
        this.diagnostic = diagnostic;
    }

    /**
     * Returns the structured {@link Diagnostic} associated with this exception.
     */
    public Diagnostic getDiagnostic() {
        return diagnostic;
    }

    /**
     * Returns the {@link DiagnosticCode} of the underlying diagnostic.
     */
    public DiagnosticCode getCode() {
        return diagnostic.code();
    }

    /**
     * Returns the {@link CompilerPhase} where the failure occurred.
     */
    public CompilerPhase getPhase() {
        return diagnostic.phase();
    }

    /**
     * Returns the optional {@link SourceLocation} associated with this failure.
     */
    public Optional<SourceLocation> getSourceLocation() {
        return diagnostic.location();
    }

    private static String formatExceptionMessage(Diagnostic diagnostic) {
        if (diagnostic == null) {
            throw new IllegalArgumentException("diagnostic must not be null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(diagnostic.code().code()).append("] ")
                .append(diagnostic.message());
        diagnostic.location().ifPresent(loc -> sb.append(" [at ")
                .append(loc.formatCompact())
                .append(", offset=").append(loc.offset()).append(']'));
        return sb.toString();
    }
}
