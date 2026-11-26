package rkcc.compiler.diagnostics;

/**
 * Severity levels for compiler diagnostics across all compilation phases.
 */
public enum DiagnosticSeverity {

    /**
     * Fatal compilation issue that prevents successful compilation output.
     */
    ERROR,

    /**
     * Potentially problematic code or construct that does not halt compilation.
     */
    WARNING,

    /**
     * Informational feedback emitted during compilation.
     */
    INFO;

    /**
     * Returns {@code true} if this severity represents a compilation failure.
     */
    public boolean isError() {
        return this == ERROR;
    }

    /**
     * Returns {@code true} if this severity represents a warning.
     */
    public boolean isWarning() {
        return this == WARNING;
    }

    /**
     * Returns {@code true} if this severity represents informational output.
     */
    public boolean isInfo() {
        return this == INFO;
    }
}
