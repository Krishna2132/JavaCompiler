package rkcc.compiler.integration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import rkcc.compiler.ast.AstNode;
import rkcc.compiler.core.AbstractTypeParser;
import rkcc.compiler.core.AssemblyBackend;
import rkcc.compiler.core.AssemblyGenerationContext;
import rkcc.compiler.core.TypeRegistry;
import rkcc.compiler.diagnostics.SourceLocation;
import rkcc.compiler.lexer.Token;
import rkcc.compiler.lexer.TokenStream;
import rkcc.compiler.lexer.TokenType;
import rkcc.compiler.types.TypeCategory;
import rkcc.compiler.types.TypeDescriptor;

/**
 * Reusable test-only fixtures, source snippets, AST doubles, type parser doubles,
 * and assembly backend stubs for validating the shared compiler foundation.
 *
 * <p>All classes here live strictly under {@code src/test} and are never part of
 * production compiler artifacts.</p>
 */
final class FoundationTestFixtures {

    static final String INT_ARITHMETIC_PROGRAM =
            "int a = 10;\n"
            + "int b = 20;\n"
            + "int c = a + b;";

    static final String INTEGRAL_WIDTHS_PROGRAM =
            "byte x = 127;\n"
            + "short y = 32000;\n"
            + "long z = 100000L;";

    static final String BOOLEAN_DECLARATION = "boolean result = true;";
    static final String STRING_DECLARATION = "String name = \"RKCC\";";
    static final String INVALID_LEXICAL_SOURCE = "int value = 10 @ 20;";

    static final TypeDescriptor BYTE_DESC = primitive("byte", TypeCategory.INTEGRAL, 1, true);
    static final TypeDescriptor SHORT_DESC = primitive("short", TypeCategory.INTEGRAL, 2, true);
    static final TypeDescriptor INT_DESC = primitive("int", TypeCategory.INTEGRAL, 4, true);
    static final TypeDescriptor LONG_DESC = primitive("long", TypeCategory.INTEGRAL, 8, true);
    static final TypeDescriptor BOOLEAN_DESC = primitive("boolean", TypeCategory.BOOLEAN, 1, false);
    static final TypeDescriptor STRING_DESC = reference("String", TypeCategory.STRING, 8);

    private FoundationTestFixtures() {
    }

    static TypeDescriptor primitive(String name, TypeCategory category, int byteSize, boolean signed) {
        return new FixtureTypeDescriptor(name, category, byteSize, signed, true);
    }

    static TypeDescriptor reference(String name, TypeCategory category, int byteSize) {
        return new FixtureTypeDescriptor(name, category, byteSize, false, false);
    }

    static TypeRegistry createPopulatedTestRegistry(AssemblyBackend backend) {
        TypeRegistry registry = new TypeRegistry();
        registry.register(new FixtureIntegralFamilyParser(backend));
        registry.register(new FixtureSingleTypeParser(BOOLEAN_DESC, TokenType.BOOLEAN, backend));
        registry.register(new FixtureSingleTypeParser(STRING_DESC, TokenType.IDENTIFIER, backend));
        return registry;
    }

    static final class FixtureAstNode implements AstNode {
        private final String nodeKind;
        private final String lexeme;
        private final TypeDescriptor typeDescriptor;
        private final SourceLocation location;

        FixtureAstNode(
                String nodeKind,
                String lexeme,
                TypeDescriptor typeDescriptor,
                SourceLocation location) {
            this.nodeKind = nodeKind;
            this.lexeme = lexeme;
            this.typeDescriptor = typeDescriptor;
            this.location = location;
        }

        @Override
        public String nodeKind() {
            return nodeKind;
        }

        @Override
        public Optional<TypeDescriptor> typeDescriptor() {
            return Optional.ofNullable(typeDescriptor);
        }

        @Override
        public Optional<SourceLocation> location() {
            return Optional.ofNullable(location);
        }

        String lexeme() {
            return lexeme;
        }
    }

    static final class StubAssemblyBackend implements AssemblyBackend {
        private final String targetName;
        private AstNode lastNode;
        private TypeDescriptor lastDescriptor;
        private AssemblyGenerationContext lastContext;

        StubAssemblyBackend(String targetName) {
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
            return "; test-double-emit [" + targetName + "] "
                    + typeDescriptor.typeName() + " " + node.nodeKind();
        }

        AstNode lastNode() {
            return lastNode;
        }

        TypeDescriptor lastDescriptor() {
            return lastDescriptor;
        }

        AssemblyGenerationContext lastContext() {
            return lastContext;
        }
    }

    static final class FixtureIntegralFamilyParser extends AbstractTypeParser<FixtureAstNode> {
        private static final Map<String, TypeDescriptor> INTEGRALS = Map.of(
                "byte", BYTE_DESC,
                "short", SHORT_DESC,
                "int", INT_DESC,
                "long", LONG_DESC
        );

        FixtureIntegralFamilyParser(AssemblyBackend backend) {
            super(backend);
        }

        @Override
        public TypeDescriptor getTypeDescriptor() {
            return INT_DESC;
        }

        @Override
        public Set<String> getSupportedTypeNames() {
            return Set.copyOf(INTEGRALS.keySet());
        }

        @Override
        protected boolean supportsCurrentTokens(TokenStream tokens) {
            Token current = tokens.peek();
            return current.isPrimitiveTypeKeyword() && INTEGRALS.containsKey(current.lexeme());
        }

        @Override
        protected FixtureAstNode parseTypeConstruct(TokenStream tokens) {
            Token typeToken = tokens.advance();
            TypeDescriptor resolved = INTEGRALS.get(typeToken.lexeme());
            return new FixtureAstNode(
                    "TestTypeKeyword",
                    typeToken.lexeme(),
                    resolved,
                    typeToken.location());
        }
    }

    static final class FixtureSingleTypeParser extends AbstractTypeParser<FixtureAstNode> {
        private final TypeDescriptor descriptor;
        private final TokenType expectedTokenType;

        FixtureSingleTypeParser(
                TypeDescriptor descriptor,
                TokenType expectedTokenType,
                AssemblyBackend backend) {
            super(backend);
            this.descriptor = descriptor;
            this.expectedTokenType = expectedTokenType;
        }

        @Override
        public TypeDescriptor getTypeDescriptor() {
            return descriptor;
        }

        @Override
        protected boolean supportsCurrentTokens(TokenStream tokens) {
            Token current = tokens.peek();
            return current.is(expectedTokenType) && descriptor.typeName().equals(current.lexeme());
        }

        @Override
        protected FixtureAstNode parseTypeConstruct(TokenStream tokens) {
            Token typeToken = tokens.advance();
            return new FixtureAstNode(
                    "TestTypeKeyword",
                    typeToken.lexeme(),
                    descriptor,
                    typeToken.location());
        }
    }

    private record FixtureTypeDescriptor(
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

    static List<String> expectedArithmeticSampleLexemes() {
        return List.of("int", "a", "=", "10", ";", "int", "b", "=", "20", ";", "int", "c", "=", "a", "+", "b", ";");
    }
}
