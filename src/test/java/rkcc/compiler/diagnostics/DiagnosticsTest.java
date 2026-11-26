package rkcc.compiler.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link Diagnostic}, {@link DiagnosticSeverity},
 * {@link CompilerPhase}, {@link DiagnosticCode}, {@link DiagnosticCollector},
 * {@link DiagnosticFormatter}, and {@link CompilerException}.
 */
class DiagnosticsTest {

    @Test
    void verifiesDiagnosticCreationEqualityAndValidation() {
        SourceLocation loc = new SourceLocation(1, 12, 11);
        Diagnostic d1 = new Diagnostic(
                DiagnosticCode.LEX_UNEXPECTED_CHARACTER,
                DiagnosticSeverity.ERROR,
                CompilerPhase.LEXICAL_ANALYSIS,
                "  Unexpected character '@'  ",
                loc,
                "  Remove invalid symbol  ");
        Diagnostic d2 = new Diagnostic(
                DiagnosticCode.LEX_UNEXPECTED_CHARACTER,
                DiagnosticSeverity.ERROR,
                CompilerPhase.LEXICAL_ANALYSIS,
                "Unexpected character '@'",
                Optional.of(new SourceLocation(1, 12, 11)),
                Optional.of("Remove invalid symbol"));
        Diagnostic withoutLoc = Diagnostic.error(
                DiagnosticCode.GEN_UNSUPPORTED_TARGET,
                "Target 'arm64' is not supported",
                null);

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertEquals("Unexpected character '@'", d1.message());
        assertEquals(Optional.of("Remove invalid symbol"), d1.details());
        assertEquals(Optional.of(loc), d1.location());
        assertNotEquals(d1, withoutLoc);
        assertTrue(withoutLoc.location().isEmpty());
        assertTrue(d1.toString().contains("LEX001"));

        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(
                null, DiagnosticSeverity.ERROR, CompilerPhase.INTERNAL, "msg", SourceLocation.START));
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(
                DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, null, CompilerPhase.INTERNAL, "msg", SourceLocation.START));
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(
                DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, DiagnosticSeverity.ERROR, null, "msg", SourceLocation.START));
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(
                DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, DiagnosticSeverity.ERROR, CompilerPhase.INTERNAL, null, SourceLocation.START));
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(
                DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, DiagnosticSeverity.ERROR, CompilerPhase.INTERNAL, "   ", SourceLocation.START));
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(
                DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, DiagnosticSeverity.ERROR, CompilerPhase.INTERNAL, "msg", (Optional<SourceLocation>) null, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(
                DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, DiagnosticSeverity.ERROR, CompilerPhase.INTERNAL, "msg", Optional.empty(), null));
    }

    @Test
    void verifiesDiagnosticCodesSeveritiesAndCompilerPhases() {
        assertTrue(DiagnosticSeverity.ERROR.isError());
        assertFalse(DiagnosticSeverity.ERROR.isWarning());
        assertTrue(DiagnosticSeverity.WARNING.isWarning());
        assertTrue(DiagnosticSeverity.INFO.isInfo());

        assertEquals("Lexical Analysis", CompilerPhase.LEXICAL_ANALYSIS.displayName());
        assertEquals("Syntax Analysis", CompilerPhase.SYNTAX_ANALYSIS.displayName());
        assertEquals("Semantic Analysis", CompilerPhase.SEMANTIC_ANALYSIS.displayName());
        assertEquals("Type Checking", CompilerPhase.TYPE_CHECKING.displayName());
        assertEquals("Code Generation", CompilerPhase.CODE_GENERATION.displayName());
        assertEquals("Internal", CompilerPhase.INTERNAL.displayName());

        assertEquals(Optional.of(DiagnosticCode.LEX_UNEXPECTED_CHARACTER), DiagnosticCode.fromCode("LEX001"));
        assertEquals(Optional.of(DiagnosticCode.SYN_UNEXPECTED_TOKEN), DiagnosticCode.fromCode("SYN001"));
        assertEquals(Optional.of(DiagnosticCode.SEM_UNDEFINED_VARIABLE), DiagnosticCode.fromCode("SEM001"));
        assertEquals(Optional.of(DiagnosticCode.GEN_UNSUPPORTED_TARGET), DiagnosticCode.fromCode("GEN003"));
        assertEquals(Optional.of(DiagnosticCode.INT_INTERNAL_COMPILER_ERROR), DiagnosticCode.fromCode("INT001"));
        assertTrue(DiagnosticCode.fromCode("NOPE").isEmpty());
        assertTrue(DiagnosticCode.fromCode(null).isEmpty());
    }

    @Test
    void collectsFiltersCountsPreservesOrderAndIsolatesSessions() {
        DiagnosticCollector sessionA = new DiagnosticCollector();
        DiagnosticCollector sessionB = new DiagnosticCollector();

        assertTrue(sessionA.isEmpty());
        assertFalse(sessionA.hasErrors());
        assertFalse(sessionA.hasWarnings());

        Diagnostic err1 = Diagnostic.error(DiagnosticCode.LEX_UNEXPECTED_CHARACTER, "Bad char", new SourceLocation(1, 5, 4));
        Diagnostic warn1 = Diagnostic.warning(DiagnosticCode.SEM_UNDEFINED_VARIABLE, "Unused warning", new SourceLocation(2, 1, 10));
        Diagnostic info1 = Diagnostic.info(DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, "Pass finished", null);
        Diagnostic err2 = Diagnostic.error(DiagnosticCode.SYN_UNEXPECTED_TOKEN, "Missing ';'", new SourceLocation(3, 8, 22));

        sessionA.report(err1);
        sessionA.report(warn1);
        sessionA.report(info1);
        sessionA.report(err2);

        assertFalse(sessionA.isEmpty());
        assertTrue(sessionA.hasErrors());
        assertTrue(sessionA.hasWarnings());
        assertEquals(4, sessionA.size());
        assertEquals(2, sessionA.countBySeverity(DiagnosticSeverity.ERROR));
        assertEquals(1, sessionA.countBySeverity(DiagnosticSeverity.WARNING));
        assertEquals(1, sessionA.countBySeverity(DiagnosticSeverity.INFO));

        assertEquals(List.of(err1, warn1, info1, err2), sessionA.getDiagnostics());
        assertEquals(List.of(err1, err2), sessionA.getErrors());
        assertEquals(List.of(warn1), sessionA.getWarnings());
        assertEquals(List.of(info1), sessionA.getInfoDiagnostics());

        assertTrue(sessionB.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> sessionA.report(null));
        assertThrows(UnsupportedOperationException.class, () -> sessionA.getDiagnostics().add(err1));
        assertThrows(UnsupportedOperationException.class, () -> sessionA.getErrors().clear());

        sessionA.clear();
        assertTrue(sessionA.isEmpty());
        assertEquals(0, sessionA.size());
    }

    @Test
    void formatsSingleLineMultilineTabsCrlfEofAndMissingSource() {
        DiagnosticFormatter formatter = new DiagnosticFormatter();

        // Single-line caret alignment
        String singleLine = "int a = 10 @ 20;";
        Diagnostic lexErr = Diagnostic.error(
                DiagnosticCode.LEX_UNEXPECTED_CHARACTER,
                "Unexpected character '@'",
                new SourceLocation(1, 12, 11));
        String formattedSingle = formatter.format(lexErr, singleLine);
        assertEquals(
                "ERROR [LEX001] (Lexical Analysis) at line 1, column 12\n"
                        + "Unexpected character '@'\n\n"
                        + "int a = 10 @ 20;\n"
                        + "           ^",
                formattedSingle);

        // Multiline with Windows CRLF and leading tab
        String multiLineCrlf = "int x = 1;\r\n\tint y = @;\r\n";
        Diagnostic tabErr = Diagnostic.error(
                DiagnosticCode.LEX_UNEXPECTED_CHARACTER,
                "Unexpected character '@'",
                new SourceLocation(2, 10, 21));
        String formattedMulti = formatter.format(tabErr, multiLineCrlf);
        assertEquals(
                "ERROR [LEX001] (Lexical Analysis) at line 2, column 10\n"
                        + "Unexpected character '@'\n\n"
                        + "\tint y = @;\n"
                        + "\t        ^",
                formattedMulti);

        // EOF at trailing empty line
        Diagnostic eofErr = Diagnostic.error(
                DiagnosticCode.SYN_UNEXPECTED_END_OF_INPUT,
                "Expected ';'",
                new SourceLocation(3, 1, 24));
        String formattedEof = formatter.format(eofErr, multiLineCrlf);
        assertTrue(formattedEof.endsWith("\n\n\n^"));

        // Missing source text and missing location
        assertEquals(
                "ERROR [LEX001] (Lexical Analysis) at line 1, column 12\nUnexpected character '@'",
                formatter.format(lexErr, null));

        Diagnostic noLoc = Diagnostic.error(DiagnosticCode.GEN_UNSUPPORTED_TARGET, "Unsupported target", null);
        assertEquals(
                "ERROR [GEN003] (Code Generation)\nUnsupported target",
                formatter.format(noLoc, singleLine));
    }

    @Test
    void preservesDiagnosticAndCauseInCompilerException() {
        Diagnostic diagnostic = Diagnostic.error(
                DiagnosticCode.INT_INTERNAL_COMPILER_ERROR,
                CompilerPhase.INTERNAL,
                "Pipeline invariant broken",
                null,
                "Retry or report bug");
        IllegalStateException rootCause = new IllegalStateException("root");
        CompilerException exception = new CompilerException(diagnostic, rootCause);

        assertSame(diagnostic, exception.getDiagnostic());
        assertEquals(DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, exception.getCode());
        assertEquals(CompilerPhase.INTERNAL, exception.getPhase());
        assertTrue(exception.getSourceLocation().isEmpty());
        assertSame(rootCause, exception.getCause());
        assertTrue(exception.getMessage().contains("INT001"));
    }
}
