package rkcc.compiler.diagnostics;

import java.util.ArrayList;
import java.util.List;

/**
 * Session-scoped diagnostic accumulator preserving insertion order and returning
 * unmodifiable snapshots so callers cannot mutate internal collector state.
 */
public final class DiagnosticCollector {

    private final List<Diagnostic> diagnostics = new ArrayList<>();

    /**
     * Records a non-null {@link Diagnostic} in insertion order.
     *
     * @param diagnostic diagnostic to record
     * @throws IllegalArgumentException if {@code diagnostic} is {@code null}
     */
    public void report(Diagnostic diagnostic) {
        if (diagnostic == null) {
            throw new IllegalArgumentException("diagnostic must not be null");
        }
        diagnostics.add(diagnostic);
    }

    /**
     * Convenience helper to report an {@link DiagnosticSeverity#ERROR} diagnostic.
     */
    public void reportError(DiagnosticCode code, String message, SourceLocation location) {
        report(Diagnostic.error(code, message, location));
    }

    /**
     * Convenience helper to report a {@link DiagnosticSeverity#WARNING} diagnostic.
     */
    public void reportWarning(DiagnosticCode code, String message, SourceLocation location) {
        report(Diagnostic.warning(code, message, location));
    }

    /**
     * Convenience helper to report an {@link DiagnosticSeverity#INFO} diagnostic.
     */
    public void reportInfo(DiagnosticCode code, String message, SourceLocation location) {
        report(Diagnostic.info(code, message, location));
    }

    /**
     * Returns an unmodifiable snapshot of all collected diagnostics in insertion order.
     */
    public List<Diagnostic> getDiagnostics() {
        return List.copyOf(diagnostics);
    }

    /**
     * Returns an unmodifiable list of {@link DiagnosticSeverity#ERROR} diagnostics.
     */
    public List<Diagnostic> getErrors() {
        return getBySeverity(DiagnosticSeverity.ERROR);
    }

    /**
     * Returns an unmodifiable list of {@link DiagnosticSeverity#WARNING} diagnostics.
     */
    public List<Diagnostic> getWarnings() {
        return getBySeverity(DiagnosticSeverity.WARNING);
    }

    /**
     * Returns an unmodifiable list of {@link DiagnosticSeverity#INFO} diagnostics.
     */
    public List<Diagnostic> getInfoDiagnostics() {
        return getBySeverity(DiagnosticSeverity.INFO);
    }

    /**
     * Returns an unmodifiable list of diagnostics matching {@code severity}.
     */
    public List<Diagnostic> getBySeverity(DiagnosticSeverity severity) {
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        return diagnostics.stream()
                .filter(d -> d.severity() == severity)
                .toList();
    }

    /**
     * Returns the total number of collected diagnostics.
     */
    public int size() {
        return diagnostics.size();
    }

    /**
     * Returns the count of diagnostics having {@code severity}.
     */
    public int countBySeverity(DiagnosticSeverity severity) {
        if (severity == null) {
            throw new IllegalArgumentException("severity must not be null");
        }
        int count = 0;
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.severity() == severity) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns {@code true} if at least one error-severity diagnostic was reported.
     */
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity().isError());
    }

    /**
     * Returns {@code true} if at least one warning-severity diagnostic was reported.
     */
    public boolean hasWarnings() {
        return diagnostics.stream().anyMatch(d -> d.severity().isWarning());
    }

    /**
     * Returns {@code true} if no diagnostics have been recorded.
     */
    public boolean isEmpty() {
        return diagnostics.isEmpty();
    }

    /**
     * Removes all collected diagnostics from this session collector.
     */
    public void clear() {
        diagnostics.clear();
    }
}
