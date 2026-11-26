package rkcc.compiler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import rkcc.compiler.ast.AstNode;
import rkcc.compiler.diagnostics.CompilerException;
import rkcc.compiler.diagnostics.CompilerPhase;
import rkcc.compiler.diagnostics.DiagnosticCode;
import rkcc.compiler.diagnostics.DiagnosticCollector;
import rkcc.compiler.diagnostics.SourceLocation;
import rkcc.compiler.lexer.Token;
import rkcc.compiler.lexer.TokenStream;
import rkcc.compiler.lexer.TokenType;
import rkcc.compiler.types.TypeCategory;
import rkcc.compiler.types.TypeDescriptor;

/**
 * Comprehensive unit and example tests for {@link TypeRegistry} and multi-type
 * {@link AbstractTypeParser} integration using test-only parser implementations.
 */
class TypeRegistryTest {

    private static final TypeDescriptor INT_DESC = descriptor("int", TypeCategory.INTEGRAL, 4, true, true);
    private static final TypeDescriptor BOOLEAN_DESC = descriptor("boolean", TypeCategory.BOOLEAN, 1, false, true);
    private static final TypeDescriptor DOUBLE_DESC = descriptor("double", TypeCategory.FLOATING_POINT, 8, true, true);
    private static final TypeDescriptor CHAR_DESC = descriptor("char", TypeCategory.CHAR, 2, false, true);
    private static final TypeDescriptor STRING_DESC = descriptor("String", TypeCategory.STRING, 8, false, false);
    private static final TypeDescriptor INT_ARRAY_DESC = descriptor("int[]", TypeCategory.ARRAY, 8, false, false);
    private static final TypeDescriptor VOID_DESC = descriptor("void", TypeCategory.VOID, 0, false, false);

    @Test
    void createsEmptyRegistryAndSupportsRegistrationUnregistrationAndSnapshots() {
        TypeRegistry registry = new TypeRegistry();

        assertTrue(registry.isEmpty());
        assertEquals(0, registry.size());
        assertTrue(registry.getRegisteredTypes().isEmpty());
        assertTrue(registry.getRegisteredParsers().isEmpty());

        TestParser booleanParser = new TestParser(BOOLEAN_DESC);
        TestParser voidParser = new TestParser(VOID_DESC);

        registry.register(booleanParser);
        registry.register(voidParser);

        assertFalse(registry.isEmpty());
        assertEquals(2, registry.size());
        assertTrue(registry.supports("boolean"));
        assertTrue(registry.supports("void"));
        assertFalse(registry.supports("int"));
        assertEquals(Set.of("boolean", "void"), registry.getRegisteredTypes());
        assertEquals(Set.of(booleanParser, voidParser), registry.getRegisteredParsers());

        assertThrows(UnsupportedOperationException.class,
                () -> registry.getRegisteredTypes().add("int"));
        assertThrows(UnsupportedOperationException.class,
                () -> registry.getRegisteredParsers().clear());

        assertEquals(Optional.of(voidParser), registry.unregister("void"));
        assertFalse(registry.supports("void"));
        assertEquals(1, registry.size());
        assertTrue(registry.unregister("void").isEmpty());
    }

    @Test
    void supportsCaseSensitiveLookupAndAllJavaTypeCategories() {
        TypeRegistry registry = new TypeRegistry();
        TestParser intParser = new TestParser(INT_DESC);
        TestParser doubleParser = new TestParser(DOUBLE_DESC);
        TestParser charParser = new TestParser(CHAR_DESC);
        TestParser stringParser = new TestParser(STRING_DESC);
        TestParser arrayParser = new TestParser(INT_ARRAY_DESC);

        registry.register(intParser);
        registry.register(doubleParser);
        registry.register(charParser);
        registry.register(stringParser);
        registry.register(arrayParser);

        assertTrue(registry.supports("int"));
        assertFalse(registry.supports("Int"));
        assertFalse(registry.supports("INT"));
        assertTrue(registry.supports("String"));
        assertFalse(registry.supports("string"));
        assertTrue(registry.supports("int[]"));

        assertTrue(TypeCategory.INTEGRAL.isPrimitiveCategory());
        assertTrue(TypeCategory.CHAR.isPrimitiveCategory());
        assertTrue(TypeCategory.FLOATING_POINT.isPrimitiveCategory());
        assertTrue(TypeCategory.BOOLEAN.isPrimitiveCategory());
        assertTrue(TypeCategory.STRING.isReferenceCategory());
        assertTrue(TypeCategory.ARRAY.isReferenceCategory());
        assertTrue(TypeCategory.REFERENCE.isReferenceCategory());
        assertTrue(TypeCategory.VOID.isVoid());
        assertFalse(TypeCategory.VOID.isPrimitiveCategory());
        assertFalse(TypeCategory.STRING.isPrimitiveCategory());
    }

    @Test
    void enforcesAtomicMultiTypeRegistrationAndRejectsDuplicatesConflictsAndInvalidInputs() {
        TypeRegistry registry = new TypeRegistry();
        TestParser booleanParser = new TestParser(BOOLEAN_DESC);
        registry.register(booleanParser);

        // Null / blank / malformed inputs
        assertThrows(IllegalArgumentException.class, () -> registry.register((AbstractTypeParser<?>) null));
        assertThrows(IllegalArgumentException.class, () -> registry.register((String) null, booleanParser));
        assertThrows(IllegalArgumentException.class, () -> registry.register((java.util.Collection<String>) null, booleanParser));
        assertThrows(IllegalArgumentException.class, () -> registry.register("", booleanParser));
        assertThrows(IllegalArgumentException.class, () -> registry.register("  ", booleanParser));
        assertThrows(IllegalArgumentException.class, () -> registry.register("123bad", booleanParser));
        assertThrows(IllegalArgumentException.class, () -> registry.register("int", null));
        assertThrows(IllegalArgumentException.class, () -> registry.register(List.of(), booleanParser));
        assertThrows(IllegalArgumentException.class, () -> registry.resolve((String) null));
        assertThrows(IllegalArgumentException.class, () -> registry.resolve(""));
        assertThrows(IllegalArgumentException.class, () -> registry.resolve((Token) null));

        // Duplicate same-parser registration rejected with INTERNAL CompilerException
        DiagnosticCollector diagnostics = new DiagnosticCollector();
        CompilerException dupEx = assertThrows(
                CompilerException.class,
                () -> registry.register(booleanParser, diagnostics));
        assertEquals(DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, dupEx.getCode());
        assertEquals(CompilerPhase.INTERNAL, dupEx.getPhase());
        assertEquals(1, diagnostics.size());

        // Atomic multi-type registration: if second type ("boolean") conflicts, first ("int") must NOT be added
        TestMultiTypeParser partialConflictParser = new TestMultiTypeParser(
                INT_DESC, Set.of("int", "boolean"));
        CompilerException conflictEx = assertThrows(
                CompilerException.class,
                () -> registry.register(partialConflictParser));
        assertEquals(DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, conflictEx.getCode());
        assertFalse(registry.supports("int"));
        assertEquals(1, registry.size());
        assertSame(booleanParser, registry.resolve("boolean").orElseThrow());

        // Incompatible parser / invalid descriptor rejected cleanly
        TestParser intParser = new TestParser(INT_DESC);
        CompilerException mismatchEx = assertThrows(
                CompilerException.class,
                () -> registry.register("String", intParser));
        assertEquals(DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, mismatchEx.getCode());

        AbstractTypeParser<AstNode> brokenParser = new AbstractTypeParser<>() {
            @Override
            public TypeDescriptor getTypeDescriptor() {
                return null;
            }

            @Override
            protected boolean supportsCurrentTokens(TokenStream tokens) {
                return false;
            }

            @Override
            protected AstNode parseTypeConstruct(TokenStream tokens) {
                return null;
            }
        };
        assertThrows(CompilerException.class, () -> registry.register(brokenParser));
    }

    @Test
    void keepsRegistryInstancesIndependent() {
        TypeRegistry registryA = new TypeRegistry();
        TypeRegistry registryB = new TypeRegistry();

        TestParser intParser = new TestParser(INT_DESC);
        registryA.register(intParser);

        assertTrue(registryA.supports("int"));
        assertFalse(registryB.supports("int"));
        assertTrue(registryB.isEmpty());
    }

    @Test
    void resolvesTokensSafelyWithoutMisclassifyingArbitraryIdentifiers() {
        TypeRegistry registry = new TypeRegistry();
        TestMultiTypeParser integralParser = new TestMultiTypeParser(
                INT_DESC, Set.of("byte", "short", "int", "long"));
        TestParser booleanParser = new TestParser(BOOLEAN_DESC);
        TestParser stringParser = new TestParser(STRING_DESC);
        TestParser arbitraryNamedParser = new TestParser(
                descriptor("CustomType", TypeCategory.REFERENCE, 8, false, false));

        registry.register(integralParser);
        registry.register(booleanParser);
        registry.register(stringParser);
        registry.register(arbitraryNamedParser);

        Token intTok = new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0));
        Token longTok = new Token(TokenType.LONG, "long", new SourceLocation(2, 1, 10));
        Token boolTok = new Token(TokenType.BOOLEAN, "boolean", new SourceLocation(3, 1, 20));
        Token strTok = new Token(TokenType.IDENTIFIER, "String", new SourceLocation(4, 1, 30));
        Token identTok = new Token(TokenType.IDENTIFIER, "CustomType", new SourceLocation(5, 1, 40));
        Token plusTok = new Token(TokenType.PLUS, "+", new SourceLocation(6, 1, 50));

        assertSame(integralParser, registry.resolve(intTok).orElseThrow());
        assertSame(integralParser, registry.resolve(longTok).orElseThrow());
        assertSame(booleanParser, registry.resolve(boolTok).orElseThrow());
        assertSame(stringParser, registry.resolve(strTok).orElseThrow());

        // Arbitrary identifiers and non-type tokens must NOT be resolved via token lookup
        assertTrue(registry.resolve(identTok).isEmpty());
        assertTrue(registry.resolve(plusTok).isEmpty());
        assertFalse(registry.supports(identTok));

        DiagnosticCollector diagnostics = new DiagnosticCollector();
        SourceLocation unknownLoc = new SourceLocation(7, 5, 65);
        Token doubleTok = new Token(TokenType.DOUBLE, "double", unknownLoc);
        assertTrue(registry.resolve(doubleTok, diagnostics).isEmpty());
        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticCode.TYP_UNSUPPORTED_TYPE, diagnostics.getErrors().get(0).code());
        assertEquals(CompilerPhase.TYPE_CHECKING, diagnostics.getErrors().get(0).phase());
        assertEquals(Optional.of(unknownLoc), diagnostics.getErrors().get(0).location());
    }

    @Test
    void executesAllMilestoneIntegrationExamplesUsingTestDoubles() {
        // Example 1: Register test integer parser supporting byte, short, int, long; resolve int
        TypeRegistry registry = new TypeRegistry();
        TestMultiTypeParser integerFamilyParser = new TestMultiTypeParser(
                INT_DESC, Set.of("byte", "short", "int", "long"));
        registry.register(integerFamilyParser);

        assertEquals(4, registry.size());
        assertEquals(1, registry.getRegisteredParsers().size());
        assertSame(integerFamilyParser, registry.resolve("int").orElseThrow());
        assertSame(integerFamilyParser, registry.resolve("byte").orElseThrow());
        assertSame(integerFamilyParser, registry.resolve("short").orElseThrow());
        assertSame(integerFamilyParser, registry.resolve("long").orElseThrow());

        // Example 2: Register test boolean parser; resolve boolean
        TestParser booleanParser = new TestParser(BOOLEAN_DESC);
        registry.register(booleanParser);
        assertSame(booleanParser, registry.resolve("boolean").orElseThrow());

        // Example 3: Register test String parser; resolve via IDENTIFIER token "String"
        TestParser stringParser = new TestParser(STRING_DESC);
        registry.register(stringParser);
        Token stringToken = new Token(TokenType.IDENTIFIER, "String", new SourceLocation(1, 1, 0));
        assertSame(stringParser, registry.resolve(stringToken).orElseThrow());

        // Example 4: Attempt to resolve unregistered type -> TYP_UNSUPPORTED_TYPE diagnostic
        DiagnosticCollector diagnostics = new DiagnosticCollector();
        assertTrue(registry.resolve("float", diagnostics).isEmpty());
        assertEquals(DiagnosticCode.TYP_UNSUPPORTED_TYPE, diagnostics.getErrors().get(0).code());
        CompilerException missingEx = assertThrows(
                CompilerException.class,
                () -> registry.resolveRequired("float"));
        assertEquals(DiagnosticCode.TYP_UNSUPPORTED_TYPE, missingEx.getCode());

        // Example 5: Attempt to register conflicting parser for "int" -> rejected, state unchanged
        TestParser conflictingIntParser = new TestParser(INT_DESC);
        CompilerException conflict = assertThrows(
                CompilerException.class,
                () -> registry.register(conflictingIntParser));
        assertEquals(DiagnosticCode.INT_INTERNAL_COMPILER_ERROR, conflict.getCode());
        assertSame(integerFamilyParser, registry.resolve("int").orElseThrow());
        assertEquals(6, registry.size());
    }

    private static TypeDescriptor descriptor(
            String typeName,
            TypeCategory category,
            int byteSize,
            boolean signed,
            boolean primitive) {
        return new TypeDescriptor() {
            @Override
            public String typeName() {
                return typeName;
            }

            @Override
            public TypeCategory category() {
                return category;
            }

            @Override
            public int byteSize() {
                return byteSize;
            }

            @Override
            public boolean isSigned() {
                return signed;
            }

            @Override
            public boolean isPrimitive() {
                return primitive;
            }

            @Override
            public boolean isAssignableTo(TypeDescriptor target) {
                return target != null && typeName.equals(target.typeName());
            }
        };
    }

    private static class TestParser extends AbstractTypeParser<AstNode> {
        private final TypeDescriptor descriptor;

        private TestParser(TypeDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public TypeDescriptor getTypeDescriptor() {
            return descriptor;
        }

        @Override
        protected boolean supportsCurrentTokens(TokenStream tokens) {
            return supports(tokens.peek().lexeme());
        }

        @Override
        protected AstNode parseTypeConstruct(TokenStream tokens) {
            Token consumed = tokens.advance();
            return new AstNode() {
                @Override
                public String nodeKind() {
                    return "TestNode:" + consumed.lexeme();
                }

                @Override
                public Optional<TypeDescriptor> typeDescriptor() {
                    return Optional.of(descriptor);
                }
            };
        }
    }

    private static final class TestMultiTypeParser extends TestParser {
        private final Set<String> supportedNames;

        private TestMultiTypeParser(TypeDescriptor primary, Set<String> supportedNames) {
            super(primary);
            this.supportedNames = Set.copyOf(supportedNames);
        }

        @Override
        public Set<String> getSupportedTypeNames() {
            return supportedNames;
        }
    }
}
