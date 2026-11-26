package rkcc.compiler.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import rkcc.compiler.ast.AstNode;
import rkcc.compiler.core.AbstractTypeParser;
import rkcc.compiler.core.AssemblyGenerationContext;
import rkcc.compiler.core.TypeRegistry;
import rkcc.compiler.diagnostics.CompilerPhase;
import rkcc.compiler.diagnostics.Diagnostic;
import rkcc.compiler.diagnostics.DiagnosticCode;
import rkcc.compiler.diagnostics.DiagnosticCollector;
import rkcc.compiler.diagnostics.DiagnosticFormatter;
import rkcc.compiler.diagnostics.SourceLocation;
import rkcc.compiler.integration.FoundationTestFixtures.FixtureAstNode;
import rkcc.compiler.integration.FoundationTestFixtures.StubAssemblyBackend;
import rkcc.compiler.lexer.Lexer;
import rkcc.compiler.lexer.LexicalException;
import rkcc.compiler.lexer.Token;
import rkcc.compiler.lexer.TokenStream;
import rkcc.compiler.lexer.TokenType;
import rkcc.compiler.lexer.UnexpectedTokenException;

/**
 * Foundation integration tests verifying cross-module interoperability across
 * {@link Lexer}, {@link TokenStream}, {@link DiagnosticCollector},
 * {@link DiagnosticFormatter}, {@link TypeRegistry}, {@link AbstractTypeParser},
 * {@link AstNode}, and test-double {@code AssemblyBackend}.
 */
class CompilerFoundationIntegrationTest {

    @Test
    void workflow1LexerToTokenStreamPreservesOrderLookaheadConsumptionAndEof() {
        Lexer lexer = new Lexer();
        TokenStream stream = lexer.tokenizeToStream(FoundationTestFixtures.INT_ARITHMETIC_PROGRAM);

        assertEquals(0, stream.position());
        assertFalse(stream.isAtEnd());
        assertEquals(TokenType.INT, stream.peek().type());
        assertEquals(TokenType.IDENTIFIER, stream.peek(1).type());
        assertEquals(TokenType.ASSIGN, stream.peek(2).type());
        assertEquals(0, stream.position());

        List<String> consumedLexemes = new ArrayList<>();
        while (!stream.isAtEnd()) {
            consumedLexemes.add(stream.advance().lexeme());
        }

        assertEquals(FoundationTestFixtures.expectedArithmeticSampleLexemes(), consumedLexemes);
        assertEquals(";", stream.previous().lexeme());
        assertTrue(stream.isAtEnd());
        assertEquals(TokenType.EOF, stream.peek().type());
        assertEquals(new SourceLocation(3, 15, 38), stream.peek().location());
    }

    @Test
    void workflow2LexerToDiagnosticsDetectsAndFormatsLexicalFailure() {
        Lexer lexer = new Lexer();
        DiagnosticCollector diagnostics = new DiagnosticCollector();
        DiagnosticFormatter formatter = new DiagnosticFormatter();

        LexicalException error = assertThrows(
                LexicalException.class,
                () -> lexer.tokenizeToStream(FoundationTestFixtures.INVALID_LEXICAL_SOURCE, diagnostics));

        assertEquals(DiagnosticCode.LEX_UNEXPECTED_CHARACTER, error.getCode());
        assertEquals(CompilerPhase.LEXICAL_ANALYSIS, error.getPhase());
        assertEquals(1, diagnostics.size());

        Diagnostic recorded = diagnostics.getErrors().get(0);
        assertEquals(Optional.of(new SourceLocation(1, 16, 15)), recorded.location());
        assertEquals(
                "ERROR [LEX001] (Lexical Analysis) at line 1, column 16\n"
                        + "Unexpected character '@'\n\n"
                        + "int value = 10 @ 20;\n"
                        + "               ^",
                formatter.format(recorded, FoundationTestFixtures.INVALID_LEXICAL_SOURCE));
    }

    @Test
    void workflow3TokenStreamToDiagnosticsReportsMismatchWithoutMovingCursor() {
        Lexer lexer = new Lexer();
        DiagnosticCollector diagnostics = new DiagnosticCollector();
        TokenStream stream = lexer.tokenizeToStream("int a = 10;", diagnostics);

        stream.consume(TokenType.INT, "Expected 'int'", diagnostics);
        stream.consume(TokenType.IDENTIFIER, "Expected identifier", diagnostics);
        stream.consume(TokenType.ASSIGN, "Expected '='", diagnostics);
        int cursorBeforeFailure = stream.position();

        UnexpectedTokenException mismatch = assertThrows(
                UnexpectedTokenException.class,
                () -> stream.consume(TokenType.SEMICOLON, "Expected ';'", diagnostics));

        assertEquals(cursorBeforeFailure, stream.position());
        assertEquals(DiagnosticCode.SYN_UNEXPECTED_TOKEN, mismatch.getCode());
        assertEquals(TokenType.SEMICOLON, mismatch.getExpectedType());
        assertEquals(TokenType.INTEGER_LITERAL, mismatch.getActualToken().type());
        assertEquals(new SourceLocation(1, 9, 8), mismatch.getLocation());
        assertEquals(1, diagnostics.size());
    }

    @Test
    void workflow4TokenStreamToTypeRegistryResolvesDeclarationTypeTokensAcrossSamples() {
        Lexer lexer = new Lexer();
        DiagnosticCollector diagnostics = new DiagnosticCollector();
        StubAssemblyBackend backend = new StubAssemblyBackend("stub-target");
        TypeRegistry registry = FoundationTestFixtures.createPopulatedTestRegistry(backend);

        // Integral widths: byte, short, long
        TokenStream integralStream = lexer.tokenizeToStream(
                FoundationTestFixtures.INTEGRAL_WIDTHS_PROGRAM, diagnostics);
        List<String> resolvedIntegralTypes = new ArrayList<>();
        while (!integralStream.isAtEnd()) {
            Token current = integralStream.peek();
            Optional<AbstractTypeParser<?>> parser = registry.resolve(current);
            if (parser.isPresent()) {
                resolvedIntegralTypes.add(current.lexeme());
                assertTrue(parser.get().supports(current.lexeme()));
            }
            integralStream.advance();
        }
        assertEquals(List.of("byte", "short", "long"), resolvedIntegralTypes);

        // Boolean declaration
        TokenStream boolStream = lexer.tokenizeToStream(
                FoundationTestFixtures.BOOLEAN_DECLARATION, diagnostics);
        Token boolToken = boolStream.peek();
        assertTrue(registry.resolve(boolToken, diagnostics).isPresent());
        boolStream.advance(); // consume 'boolean'
        Token boolIdent = boolStream.peek();
        assertTrue(registry.resolve(boolIdent).isEmpty()); // 'result' identifier is NOT a type

        // String declaration
        TokenStream stringStream = lexer.tokenizeToStream(
                FoundationTestFixtures.STRING_DECLARATION, diagnostics);
        Token stringToken = stringStream.peek();
        assertEquals(TokenType.IDENTIFIER, stringToken.type());
        assertEquals("String", stringToken.lexeme());
        assertTrue(registry.resolve(stringToken, diagnostics).isPresent());
        stringStream.advance();
        assertTrue(registry.resolve(stringStream.peek()).isEmpty()); // 'name' identifier is NOT a type

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void workflow5AbstractTypeParserIntegratesWithRegistryAstNodeAndStubBackend() {
        Lexer lexer = new Lexer();
        DiagnosticCollector diagnostics = new DiagnosticCollector();
        StubAssemblyBackend stubBackend = new StubAssemblyBackend("test-x86-stub");
        TypeRegistry registry = FoundationTestFixtures.createPopulatedTestRegistry(stubBackend);
        AssemblyGenerationContext context = new AssemblyGenerationContext(
                "x86_64-test-stub", ".text", Map.of("mode", "foundation-test"));

        TokenStream stream = lexer.tokenizeToStream(FoundationTestFixtures.STRING_DECLARATION, diagnostics);
        AbstractTypeParser<?> resolvedParser = registry.resolve(stream.peek(), diagnostics)
                .orElseThrow();

        @SuppressWarnings("unchecked")
        AbstractTypeParser<AstNode> parser = (AbstractTypeParser<AstNode>) resolvedParser;

        assertTrue(parser.supports(stream));
        AstNode node = parser.parse(stream);
        assertInstanceOf(FixtureAstNode.class, node);
        assertEquals("TestTypeKeyword", node.nodeKind());
        assertEquals("String", node.typeDescriptor().orElseThrow().typeName());
        assertEquals(Optional.of(new SourceLocation(1, 1, 0)), node.location());
        assertEquals(TokenType.IDENTIFIER, stream.peek().type());
        assertEquals("name", stream.peek().lexeme());

        String stubOutput = parser.generateAssembly(node, context);
        assertEquals("; test-double-emit [test-x86-stub] String TestTypeKeyword", stubOutput);
        assertSame(node, stubBackend.lastNode());
        assertEquals("String", stubBackend.lastDescriptor().typeName());
        assertSame(context, stubBackend.lastContext());
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void isolatesIndependentSessionsAndValidatesContextAttributes() {
        Lexer lexer = new Lexer();
        DiagnosticCollector diagA = new DiagnosticCollector();
        DiagnosticCollector diagB = new DiagnosticCollector();
        TypeRegistry regA = new TypeRegistry();
        TypeRegistry regB = new TypeRegistry();

        StubAssemblyBackend stubA = new StubAssemblyBackend("stub-a");
        regA.register(new FoundationTestFixtures.FixtureSingleTypeParser(
                FoundationTestFixtures.INT_DESC, TokenType.INT, stubA));

        assertNotSame(diagA, diagB);
        assertTrue(regA.supports("int"));
        assertFalse(regB.supports("int"));

        assertThrows(LexicalException.class,
                () -> lexer.tokenize(FoundationTestFixtures.INVALID_LEXICAL_SOURCE, diagA));
        assertEquals(1, diagA.size());
        assertTrue(diagB.isEmpty());

        Map<String, String> nullKeyMap = new HashMap<>();
        nullKeyMap.put(null, "val");
        assertThrows(IllegalArgumentException.class,
                () -> new AssemblyGenerationContext("x86_64-test", ".text", nullKeyMap));

        Map<String, String> nullValMap = new HashMap<>();
        nullValMap.put("key", null);
        assertThrows(IllegalArgumentException.class,
                () -> new AssemblyGenerationContext("x86_64-test", ".text", nullValMap));
    }
}
