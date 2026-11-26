package rkcc.compiler.diagnostics;

import java.util.Optional;

/**
 * Immutable compiler diagnostic capturing code, severity, phase, human-readable
 * message, optional {@link SourceLocation}, and optional extra details.
 *
 * @param code stable non-null {@link DiagnosticCode}
 * @param severity non-null {@link DiagnosticSeverity}
 * @param phase non-null {@link CompilerPhase}
 * @param message non-blank human-readable message
 * @param location optional source location (empty for internal or backend-wide diagnostics)
 * @param details optional supplementary explanation or suggestion
 */
public record Diagnostic(
        DiagnosticCode code,
        DiagnosticSeverity severity,
        CompilerPhase phase,
        String message,
        Optional<SourceLocation> location,
        Optional<String> details) {

    public Diagnostic {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        if (phase == null) {
            throw new IllegalArgumentException("phase must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("location Optional must not be null");
        }
        if (details == null) {
            throw new IllegalArgumentException("details Optional must not be null");
        }
        message = message.trim();
        details = details.map(String::trim).filter(s -> !s.isEmpty());
    }

    /**
     * Creates a diagnostic with an optional nullable {@link SourceLocation} and no details.
     */
    public Diagnostic(
            DiagnosticCode code,
            DiagnosticSeverity severity,
            CompilerPhase phase,
            String message,
            SourceLocation location) {
        this(code, severity, phase, message, Optional.ofNullable(location), Optional.empty());
    }

    /**
     * Creates a diagnostic with an optional nullable {@link SourceLocation} and nullable details.
     */
    public Diagnostic(
            DiagnosticCode code,
            DiagnosticSeverity severity,
            CompilerPhase phase,
            String message,
            SourceLocation location,
            String details) {
        this(code, severity, phase, message, Optional.ofNullable(location), Optional.ofNullable(details));
    }

    /**
     * Creates an {@link DiagnosticSeverity#ERROR} diagnostic using {@code code.defaultPhase()}.
     */
    public static Diagnostic error(DiagnosticCode code, String message, SourceLocation location) {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        return new Diagnostic(code, DiagnosticSeverity.ERROR, code.defaultPhase(), message, location);
    }

    /**
     * Creates an {@link DiagnosticSeverity#ERROR} diagnostic with explicit phase and optional details.
     */
    public static Diagnostic error(
            DiagnosticCode code,
            CompilerPhase phase,
            String message,
            SourceLocation location,
            String details) {
        return new Diagnostic(code, DiagnosticSeverity.ERROR, phase, message, location, details);
    }

    /**
     * Creates a {@link DiagnosticSeverity#WARNING} diagnostic using {@code code.defaultPhase()}.
     */
    public static Diagnostic warning(DiagnosticCode code, String message, SourceLocation location) {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        return new Diagnostic(code, DiagnosticSeverity.WARNING, code.defaultPhase(), message, location);
    }

    /**
     * Creates an {@link DiagnosticSeverity#INFO} diagnostic using {@code code.defaultPhase()}.
     */
    public static Diagnostic info(DiagnosticCode code, String message, SourceLocation location) {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        return new Diagnostic(code, DiagnosticSeverity.INFO, code.defaultPhase(), message, location);
    }

    @Override
    public String toString() {
        String locText = location.map(SourceLocation::formatCompact).orElse("<no-location>");
        return "Diagnostic[severity=" + severity
                + ", code=" + code.code()
                + ", phase=" + phase
                + ", location=" + locText
                + ", message='" + message + "']";
    }
}
