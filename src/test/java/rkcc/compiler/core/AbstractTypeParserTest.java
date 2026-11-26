package rkcc.compiler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import rkcc.compiler.ast.AstNode;
import rkcc.compiler.diagnostics.SourceLocation;
import rkcc.compiler.lexer.Token;
import rkcc.compiler.lexer.TokenStream;
import rkcc.compiler.lexer.TokenType;
import rkcc.compiler.types.TypeCategory;
import rkcc.compiler.types.TypeDescriptor;

/**
 * Unit tests verifying {@link AbstractTypeParser} contracts, extensibility,
 * input/output validation, backend delegation, and error propagation using
 * test-only subclasses and test doubles.
 */
class AbstractTypeParserTest {

    private static final TypeDescriptor TEST_INT_DESCRIPTOR = new TestTypeDescriptor(
            "int", TypeCategory.INTEGRAL, 4, true, true);

    private static final TypeDescriptor TEST_STRING_DESCRIPTOR = new TestTypeDescriptor(
            "String", TypeCategory.STRING, 8, false, false);

    private static final TypeDescriptor TEST_INT_ARRAY_DESCRIPTOR = new TestTypeDescriptor(
            "int[]", TypeCategory.ARRAY, 8, false, false);

    @Test
    void verifiesTypeDetectionAndMetadataAccess() {
        TestTypeParser parser = new TestTypeParser(TEST_INT_DESCRIPTOR);

        assertEquals("int", parser.getSupportedTypeName());
        assertSame(TEST_INT_DESCRIPTOR, parser.getTypeDescriptor());
        assertTrue(parser.supports("int"));
        assertTrue(parser.supports(" int "));
        assertFalse(parser.supports("double"));
        assertFalse(parser.supports("String"));
        assertFalse(parser.supports("int[]"));

        parser.requireSupportedType("int");
        assertThrows(UnsupportedOperationException.class,
                () -> parser.requireSupportedType("boolean"));
    }

    @Test
    void verifiesExtensibilityAcrossDiverseJavaTypeCategories() {
        TestTypeParser stringParser = new TestTypeParser(TEST_STRING_DESCRIPTOR);
        TestTypeParser arrayParser = new TestTypeParser(TEST_INT_ARRAY_DESCRIPTOR);

        assertTrue(stringParser.supports("String"));
        assertFalse(stringParser.supports("int"));
        assertEquals(TypeCategory.STRING, stringParser.getTypeDescriptor().category());

        assertTrue(arrayParser.supports("int[]"));
        assertFalse(arrayParser.supports("int"));
        assertEquals(TypeCategory.ARRAY, arrayParser.getTypeDescriptor().category());
    }

    @Test
    void rejectsNullAndInvalidTypeNames() {
        TestTypeParser parser = new TestTypeParser(TEST_INT_DESCRIPTOR);

        assertThrows(IllegalArgumentException.class, () -> parser.supports((String) null));
        assertThrows(IllegalArgumentException.class, () -> parser.supports(""));
        assertThrows(IllegalArgumentException.class, () -> parser.supports("   "));
        assertThrows(IllegalArgumentException.class, () -> parser.supports("123int"));
        assertThrows(IllegalArgumentException.class, () -> parser.supports("int;"));
        assertThrows(IllegalArgumentException.class, () -> parser.supports("int []"));
        assertThrows(IllegalArgumentException.class, () -> parser.requireSupportedType("9bad"));
    }

    @Test
    void validatesTokenStreamSupportAndParsesConstructs() {
        TestTypeParser parser = new TestTypeParser(TEST_INT_DESCRIPTOR);
        TokenStream validTokens = new TestTokenStream(List.of(
                new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0)),
                new Token(TokenType.IDENTIFIER, "count", new SourceLocation(1, 5, 4)),
                new Token(TokenType.SEMICOLON, ";", new SourceLocation(1, 10, 9))));
        TokenStream otherTokens = new TestTokenStream(List.of(
                new Token(TokenType.BOOLEAN, "boolean", SourceLocation.START)));
        TokenStream exhaustedTokens = new TestTokenStream(List.of());

        assertThrows(IllegalArgumentException.class, () -> parser.supports((TokenStream) null));
        assertFalse(parser.supports(exhaustedTokens));
        assertFalse(parser.supports(otherTokens));
        assertTrue(parser.supports(validTokens));

        TestAstNode parsed = parser.parse(validTokens);
        assertEquals("TestTypeConstruct", parsed.nodeKind());
        assertEquals("int", parsed.lexeme());
        assertEquals(Optional.of(TEST_INT_DESCRIPTOR), parsed.typeDescriptor());
        assertEquals(TokenType.IDENTIFIER, validTokens.peek().type());
    }

    @Test
    void rejectsNullExhaustedUnsupportedAndInvalidParseResults() {
        TestTypeParser parser = new TestTypeParser(TEST_INT_DESCRIPTOR);
        TokenStream exhaustedTokens = new TestTokenStream(List.of());
        TokenStream unsupportedTokens = new TestTokenStream(List.of(
                new Token(TokenType.FLOAT, "float", SourceLocation.START)));

        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
        assertThrows(IllegalStateException.class, () -> parser.parse(exhaustedTokens));
        assertThrows(UnsupportedOperationException.class, () -> parser.parse(unsupportedTokens));

        AbstractTypeParser<TestAstNode> nullResultParser = new AbstractTypeParser<>() {
            @Override
            public TypeDescriptor getTypeDescriptor() {
                return TEST_INT_DESCRIPTOR;
            }

            @Override
            protected boolean supportsCurrentTokens(TokenStream tokens) {
                return true;
            }

            @Override
            protected TestAstNode parseTypeConstruct(TokenStream tokens) {
                return null;
            }
        };
        assertThrows(IllegalStateException.class,
                () -> nullResultParser.parse(new TestTokenStream(List.of(
                        new Token(TokenType.INT, "int", SourceLocation.START)))));

        AbstractTypeParser<TestAstNode> mismatchedNodeParser = new AbstractTypeParser<>() {
            @Override
            public TypeDescriptor getTypeDescriptor() {
                return TEST_INT_DESCRIPTOR;
            }

            @Override
            protected boolean supportsCurrentTokens(TokenStream tokens) {
                return true;
            }

            @Override
            protected TestAstNode parseTypeConstruct(TokenStream tokens) {
                return new TestAstNode("TestNode", "hello", Optional.of(TEST_STRING_DESCRIPTOR));
            }
        };
        assertThrows(IllegalStateException.class,
                () -> mismatchedNodeParser.parse(new TestTokenStream(List.of(
                        new Token(TokenType.INT, "int", SourceLocation.START)))));

        AbstractTypeParser<TestAstNode> nullDescriptorParser = new AbstractTypeParser<>() {
            @Override
            public TypeDescriptor getTypeDescriptor() {
                return null;
            }

            @Override
            protected boolean supportsCurrentTokens(TokenStream tokens) {
                return true;
            }

            @Override
            protected TestAstNode parseTypeConstruct(TokenStream tokens) {
                return new TestAstNode("TestNode", "x", Optional.empty());
            }
        };
        assertThrows(IllegalStateException.class, nullDescriptorParser::getSupportedTypeName);
    }

    @Test
    void delegatesAssemblyGenerationToBoundBackend() {
        RecordingAssemblyBackend backend = new RecordingAssemblyBackend("test-x86-double");
        TestTypeParser parser = new TestTypeParser(TEST_INT_DESCRIPTOR, backend);
        TestAstNode node = new TestAstNode("TestTypeConstruct", "int", Optional.of(TEST_INT_DESCRIPTOR));
        AssemblyGenerationContext context = new AssemblyGenerationContext(
                "x86_64-test", ".text", Map.of("mode", "unit-test"));

        String output = parser.generateAssembly(node, context);

        assertTrue(parser.getAssemblyBackend().isPresent());
        assertEquals("test-emit:int:TestTypeConstruct:.text", output);
        assertSame(node, backend.lastNode);
        assertSame(TEST_INT_DESCRIPTOR, backend.lastDescriptor);
        assertSame(context, backend.lastContext);
    }

    @Test
    void delegatesAssemblyGenerationToExplicitBackendAndPropagatesFailures() {
        TestTypeParser parser = new TestTypeParser(TEST_INT_DESCRIPTOR);
        TestAstNode node = new TestAstNode("TestTypeConstruct", "42", Optional.empty());
        AssemblyGenerationContext context = new AssemblyGenerationContext("x86_64-test");
        RecordingAssemblyBackend backend = new RecordingAssemblyBackend("explicit-backend");

        String output = parser.generateAssembly(node, context, backend);

        assertFalse(parser.getAssemblyBackend().isPresent());
        assertEquals("test-emit:int:TestTypeConstruct:default", output);
        assertSame(TEST_INT_DESCRIPTOR, backend.lastDescriptor);

        AssemblyBackend failingBackend = new AssemblyBackend() {
            @Override
            public String targetName() {
                return "failing-backend";
            }

            @Override
            public String emitAssembly(
                    AstNode n,
                    TypeDescriptor descriptor,
                    AssemblyGenerationContext ctx) {
                throw new IllegalStateException("backend failure");
            }
        };
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> parser.generateAssembly(node, context, failingBackend));
        assertEquals("backend failure", thrown.getMessage());
    }

    @Test
    void validatesMissingBackendInvalidCodegenInputsAndEmptyBackendOutput() {
        assertThrows(IllegalArgumentException.class,
                () -> new TestTypeParser(TEST_INT_DESCRIPTOR, null));

        TestTypeParser unboundParser = new TestTypeParser(TEST_INT_DESCRIPTOR);
        TestAstNode node = new TestAstNode("TestTypeConstruct", "int", Optional.of(TEST_INT_DESCRIPTOR));
        AssemblyGenerationContext context = new AssemblyGenerationContext("x86_64-test");
        RecordingAssemblyBackend backend = new RecordingAssemblyBackend("valid-backend");

        assertThrows(IllegalStateException.class,
                () -> unboundParser.generateAssembly(node, context));
        assertThrows(IllegalArgumentException.class,
                () -> unboundParser.generateAssembly(null, context, backend));
        assertThrows(IllegalArgumentException.class,
                () -> unboundParser.generateAssembly(node, null, backend));
        assertThrows(IllegalArgumentException.class,
                () -> unboundParser.generateAssembly(node, context, null));

        TestAstNode wrongTypeNode = new TestAstNode(
                "TestTypeConstruct", "str", Optional.of(TEST_STRING_DESCRIPTOR));
        assertThrows(IllegalArgumentException.class,
                () -> unboundParser.generateAssembly(wrongTypeNode, context, backend));

        AssemblyBackend blankBackend = new AssemblyBackend() {
            @Override
            public String targetName() {
                return "blank-backend";
            }

            @Override
            public String emitAssembly(
                    AstNode n,
                    TypeDescriptor descriptor,
                    AssemblyGenerationContext ctx) {
                return "   ";
            }
        };
        assertThrows(IllegalStateException.class,
                () -> unboundParser.generateAssembly(node, context, blankBackend));
    }

    @Test
    void validatesAssemblyGenerationContextInputs() {
        assertThrows(IllegalArgumentException.class, () -> new AssemblyGenerationContext(null));
        assertThrows(IllegalArgumentException.class, () -> new AssemblyGenerationContext("  "));
        assertThrows(NullPointerException.class,
                () -> new AssemblyGenerationContext("x86_64-test", ".text", null));

        AssemblyGenerationContext context = new AssemblyGenerationContext(
                " x86_64-test ", "  ", Map.of("abi", "sysv"));
        assertEquals("x86_64-test", context.getTargetArchitecture());
        assertTrue(context.getSectionHint().isEmpty());
        assertEquals("sysv", context.getAttributes().get("abi"));
    }

    private record TestTypeDescriptor(
            String typeName,
            TypeCategory category,
            int byteSize,
            boolean isSigned,
            boolean isPrimitive) implements TypeDescriptor {

        @Override
        public boolean isAssignableTo(TypeDescriptor target) {
            return target != null && typeName.equals(target.typeName());
        }
    }

    private record TestAstNode(
            String nodeKind,
            String lexeme,
            Optional<TypeDescriptor> typeDescriptor) implements AstNode {}

    private static final class TestTokenStream implements TokenStream {
        private static final Token EOF_TOKEN = Token.eof(SourceLocation.START);
        private final List<Token> tokens;
        private int index = 0;

        private TestTokenStream(List<Token> tokens) {
            this.tokens = List.copyOf(tokens);
        }

        @Override
        public Token peek() {
            return peek(0);
        }

        @Override
        public Token peek(int offset) {
            int target = index + offset;
            if (target < 0 || target >= tokens.size()) {
                return EOF_TOKEN;
            }
            return tokens.get(target);
        }

        @Override
        public Token advance() {
            if (isAtEnd()) {
                return EOF_TOKEN;
            }
            return tokens.get(index++);
        }

        @Override
        public boolean isAtEnd() {
            return index >= tokens.size();
        }

        @Override
        public boolean match(TokenType expectedType) {
            if (!isAtEnd() && peek().is(expectedType)) {
                advance();
                return true;
            }
            return false;
        }
    }

    private static final class RecordingAssemblyBackend implements AssemblyBackend {
        private final String targetName;
        private AstNode lastNode;
        private TypeDescriptor lastDescriptor;
        private AssemblyGenerationContext lastContext;

        private RecordingAssemblyBackend(String targetName) {
            this.targetName = targetName;
        }

        @Override
        public String targetName() {
            return targetName;
        }

        @Override
        public String emitAssembly(
                AstNode node,
                TypeDescriptor typeDescriptor,
                AssemblyGenerationContext context) {
            this.lastNode = node;
            this.lastDescriptor = typeDescriptor;
            this.lastContext = context;
            return "test-emit:"
                    + typeDescriptor.typeName()
                    + ":"
                    + node.nodeKind()
                    + ":"
                    + context.getSectionHint().orElse("default");
        }
    }

    private static final class TestTypeParser extends AbstractTypeParser<TestAstNode> {
        private final TypeDescriptor descriptor;

        private TestTypeParser(TypeDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        private TestTypeParser(TypeDescriptor descriptor, AssemblyBackend backend) {
            super(backend);
            this.descriptor = descriptor;
        }

        @Override
        public TypeDescriptor getTypeDescriptor() {
            return descriptor;
        }

        @Override
        protected boolean supportsCurrentTokens(TokenStream tokens) {
            return descriptor.typeName().equals(tokens.peek().lexeme());
        }

        @Override
        protected TestAstNode parseTypeConstruct(TokenStream tokens) {
            Token consumed = tokens.advance();
            return new TestAstNode("TestTypeConstruct", consumed.lexeme(), Optional.of(descriptor));
        }
    }
}
