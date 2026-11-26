package rkcc.compiler.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import rkcc.compiler.lexer.Lexer;
import rkcc.compiler.lexer.LexicalException;
import rkcc.compiler.lexer.TokenStream;
import rkcc.compiler.lexer.TokenType;
import rkcc.compiler.lexer.UnexpectedTokenException;

/**
 * Integration tests verifying {@link Lexer} and {@link TokenStream} error
 * reporting through {@link DiagnosticCollector}, {@link DiagnosticFormatter},
 * and {@link CompilerException}.
 */
class DiagnosticIntegrationTest {

    private final Lexer lexer = new Lexer();
    private final DiagnosticFormatter formatter = new DiagnosticFormatter();

    @Test
    void example1UnexpectedCharacterProducesFormattedDiagnostic() {
        String source = "int a = 10 @ 20;";
        DiagnosticCollector collector = new DiagnosticCollector();

        LexicalException ex = assertThrows(
                LexicalException.class,
                () -> lexer.tokenizeToStream(source, collector));

        assertEquals(DiagnosticCode.LEX_UNEXPECTED_CHARACTER, ex.getCode());
        assertEquals(CompilerPhase.LEXICAL_ANALYSIS, ex.getPhase());
        assertEquals(new SourceLocation(1, 12, 11), ex.getLocation());
        assertEquals(1, collector.size());

        Diagnostic diagnostic = collector.getErrors().get(0);
        assertEquals(
                "ERROR [LEX001] (Lexical Analysis) at line 1, column 12\n"
                        + "Unexpected character '@'\n\n"
                        + "int a = 10 @ 20;\n"
                        + "           ^",
                formatter.format(diagnostic, source));
    }

    @Test
    void example2UnterminatedStringProducesLex003Diagnostic() {
        String source = "String name = \"RKCC;";
        DiagnosticCollector collector = new DiagnosticCollector();

        LexicalException ex = assertThrows(
                LexicalException.class,
                () -> lexer.tokenize(source, collector));

        assertEquals(DiagnosticCode.LEX_UNTERMINATED_STRING, ex.getCode());
        assertEquals(new SourceLocation(1, 15, 14), ex.getLocation());
        assertEquals(1, collector.countBySeverity(DiagnosticSeverity.ERROR));

        String rendered = formatter.format(collector.getErrors().get(0), source);
        assertTrue(rendered.contains("ERROR [LEX003] (Lexical Analysis) at line 1, column 15"));
        assertTrue(rendered.contains("Unterminated string literal"));
        assertTrue(rendered.contains("              ^"));
    }

    @Test
    void example3InvalidNumericLiteralProducesLex002Diagnostic() {
        String source = "int value = 0xG;";
        DiagnosticCollector collector = new DiagnosticCollector();

        LexicalException ex = assertThrows(
                LexicalException.class,
                () -> lexer.tokenize(source, collector));

        assertEquals(DiagnosticCode.LEX_INVALID_NUMERIC_LITERAL, ex.getCode());
        assertEquals(new SourceLocation(1, 13, 12), ex.getLocation());
        assertEquals(1, collector.size());

        String rendered = formatter.format(collector.getErrors().get(0), source);
        assertTrue(rendered.contains("ERROR [LEX002] (Lexical Analysis) at line 1, column 13"));
        assertTrue(rendered.contains("            ^"));
    }

    @Test
    void example4UnterminatedBlockCommentProducesLex006Diagnostic() {
        String source = "/* unfinished comment";
        DiagnosticCollector collector = new DiagnosticCollector();

        LexicalException ex = assertThrows(
                LexicalException.class,
                () -> lexer.tokenize(source, collector));

        assertEquals(DiagnosticCode.LEX_UNTERMINATED_BLOCK_COMMENT, ex.getCode());
        assertEquals(SourceLocation.START, ex.getLocation());
        assertEquals(1, collector.size());

        String rendered = formatter.format(collector.getErrors().get(0), source);
        assertTrue(rendered.contains("ERROR [LEX006] (Lexical Analysis) at line 1, column 1"));
        assertTrue(rendered.contains("/* unfinished comment\n^"));
    }

    @Test
    void example5UnexpectedTokenAndEofConsumePreserveCursorAndReportDiagnostics() {
        String source = "int count = 10";
        DiagnosticCollector collector = new DiagnosticCollector();
        TokenStream stream = lexer.tokenizeToStream(source, collector);
        assertTrue(collector.isEmpty());

        stream.consume(TokenType.INT, "Expected 'int'", collector);
        stream.consume(TokenType.IDENTIFIER, "Expected identifier", collector);
        int posBeforeMismatch = stream.position();

        UnexpectedTokenException mismatch = assertThrows(
                UnexpectedTokenException.class,
                () -> stream.consume(TokenType.SEMICOLON, "Expected ';' before assignment", collector));

        assertEquals(posBeforeMismatch, stream.position());
        assertEquals(DiagnosticCode.SYN_UNEXPECTED_TOKEN, mismatch.getCode());
        assertEquals(TokenType.SEMICOLON, mismatch.getExpectedType());
        assertEquals(TokenType.ASSIGN, mismatch.getActualToken().type());
        assertEquals(new SourceLocation(1, 11, 10), mismatch.getLocation());

        // Advance to EOF and verify SYN_UNEXPECTED_END_OF_INPUT
        stream.consume(TokenType.ASSIGN, "Expected '='", collector);
        stream.consume(TokenType.INTEGER_LITERAL, "Expected integer", collector);
        UnexpectedTokenException eofMismatch = assertThrows(
                UnexpectedTokenException.class,
                () -> stream.consume(TokenType.SEMICOLON, "Expected ';' at end of statement", collector));

        assertEquals(DiagnosticCode.SYN_UNEXPECTED_END_OF_INPUT, eofMismatch.getCode());
        assertEquals(2, collector.countBySeverity(DiagnosticSeverity.ERROR));
        assertTrue(formatter.format(collector.getErrors().get(0), source).contains("SYN001"));
        assertTrue(formatter.format(collector.getErrors().get(1), source).contains("SYN003"));
    }

    @Test
    void mapsEscapeAndCharacterLiteralErrorsToStableDiagnosticCodes() {
        DiagnosticCollector escapeCollector = new DiagnosticCollector();
        LexicalException badEscape = assertThrows(
                LexicalException.class,
                () -> lexer.tokenize("\"bad\\q\"", escapeCollector));
        assertEquals(DiagnosticCode.LEX_INVALID_ESCAPE_SEQUENCE, badEscape.getCode());
        assertEquals(1, escapeCollector.size());

        DiagnosticCollector charCollector = new DiagnosticCollector();
        LexicalException badChar = assertThrows(
                LexicalException.class,
                () -> lexer.tokenize("''", charCollector));
        assertEquals(DiagnosticCode.LEX_INVALID_CHARACTER_LITERAL, badChar.getCode());
        assertEquals(1, charCollector.size());
    }
}
