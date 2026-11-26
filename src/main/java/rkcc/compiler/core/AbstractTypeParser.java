package rkcc.compiler.core;

import java.util.Optional;
import java.util.regex.Pattern;
import rkcc.compiler.ast.AstNode;
import rkcc.compiler.lexer.TokenStream;
import rkcc.compiler.types.TypeDescriptor;

/**
 * Reusable abstract base class defining the common contract for Java data-type
 * parsing and backend assembly delegation.
 *
 * <p>Concrete subclasses (planned for future milestones) handle individual Java
 * types such as {@code int}, {@code long}, {@code boolean}, {@code String}, arrays,
 * and reference types without duplicating full grammar parsing or x86 code generation.</p>
 *
 * @param <T> specific {@link AstNode} subtype produced by this type parser
 */
public abstract class AbstractTypeParser<T extends AstNode> {

    private static final Pattern VALID_TYPE_NAME_PATTERN =
            Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*(\\[\\])*$");

    private final AssemblyBackend assemblyBackend;

    /**
     * Creates a type parser without a pre-bound {@link AssemblyBackend}.
     *
     * <p>Callers using this constructor must supply a backend explicitly to
     * {@link #generateAssembly(AstNode, AssemblyGenerationContext, AssemblyBackend)}.</p>
     */
    protected AbstractTypeParser() {
        this.assemblyBackend = null;
    }

    /**
     * Creates a type parser bound to a default {@link AssemblyBackend}.
     *
     * @param assemblyBackend non-null backend used for assembly delegation
     * @throws IllegalArgumentException if {@code assemblyBackend} is {@code null}
     */
    protected AbstractTypeParser(AssemblyBackend assemblyBackend) {
        if (assemblyBackend == null) {
            throw new IllegalArgumentException("assemblyBackend must not be null");
        }
        this.assemblyBackend = assemblyBackend;
    }

    /**
     * Returns the canonical {@link TypeDescriptor} handled by this parser.
     *
     * @return non-null type descriptor representing the supported Java type
     */
    public abstract TypeDescriptor getTypeDescriptor();

    /**
     * Checks whether the current token-stream position starts a construct
     * recognizable by this type parser.
     *
     * <p>Guaranteed to be called only when {@code tokens} is non-null and not at end.</p>
     *
     * @param tokens non-null, non-exhausted token stream
     * @return {@code true} if this parser can parse the current construct
     */
    protected abstract boolean supportsCurrentTokens(TokenStream tokens);

    /**
     * Parses a type-specific construct from the supplied {@link TokenStream}.
     *
     * <p>Implementations focus strictly on parsing type constructs into AST nodes
     * and must not perform x86 assembly generation.</p>
     *
     * @param tokens validated, non-exhausted token stream
     * @return non-null structured AST node representing the parsed construct
     */
    protected abstract T parseTypeConstruct(TokenStream tokens);

    /**
     * Returns the validated canonical type name supported by this parser.
     *
     * @return non-blank Java type name
     */
    public final String getSupportedTypeName() {
        return validatedTypeDescriptor().typeName();
    }

    /**
     * Checks whether this parser supports the specified Java type name.
     *
     * @param typeName Java type name to check (for example, {@code "int"} or {@code "int[]"})
     * @return {@code true} if {@code typeName} matches this parser's type descriptor
     * @throws IllegalArgumentException if {@code typeName} is null, blank, or syntactically invalid
     */
    public boolean supports(String typeName) {
        String normalized = validateTypeName(typeName);
        return validatedTypeDescriptor().typeName().equals(normalized);
    }

    /**
     * Checks whether this parser supports the construct at the current position of {@code tokens}.
     *
     * @param tokens token stream to inspect
     * @return {@code true} if {@code tokens} is not at end and matches this type parser
     * @throws IllegalArgumentException if {@code tokens} is {@code null}
     */
    public final boolean supports(TokenStream tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("tokens must not be null");
        }
        if (tokens.isAtEnd()) {
            return false;
        }
        return supportsCurrentTokens(tokens);
    }

    /**
     * Asserts that {@code typeName} is supported by this parser, throwing an exception otherwise.
     *
     * @param typeName Java type name to verify
     * @throws IllegalArgumentException if {@code typeName} is null, blank, or invalid
     * @throws UnsupportedOperationException if {@code typeName} is valid but unsupported
     */
    public final void requireSupportedType(String typeName) {
        String normalized = validateTypeName(typeName);
        if (!supports(normalized)) {
            throw new UnsupportedOperationException(
                    "Unsupported Java type '" + normalized + "' for parser " + getClass().getSimpleName());
        }
    }

    /**
     * Validates inputs and parses a type-specific construct from {@code tokens}.
     *
     * @param tokens non-null token stream positioned at a supported construct
     * @return validated AST node produced by {@link #parseTypeConstruct(TokenStream)}
     * @throws IllegalArgumentException if {@code tokens} is {@code null}
     * @throws IllegalStateException if {@code tokens} is exhausted or the subclass returns an invalid AST node
     * @throws UnsupportedOperationException if the current tokens are not supported by this parser
     */
    public final T parse(TokenStream tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("tokens must not be null");
        }
        TypeDescriptor descriptor = validatedTypeDescriptor();
        if (tokens.isAtEnd()) {
            throw new IllegalStateException(
                    "Cannot parse type '" + descriptor.typeName() + "' from an exhausted TokenStream");
        }
        if (!supportsCurrentTokens(tokens)) {
            throw new UnsupportedOperationException(
                    "Current token stream is not supported by parser for type '" + descriptor.typeName() + "'");
        }

        T node = parseTypeConstruct(tokens);
        validateParsedNode(node, descriptor);
        return node;
    }

    /**
     * Returns the configured default {@link AssemblyBackend}, if present.
     *
     * @return an {@link Optional} containing the configured backend, or empty if none was bound
     */
    public final Optional<AssemblyBackend> getAssemblyBackend() {
        return Optional.ofNullable(assemblyBackend);
    }

    /**
     * Delegates assembly generation for {@code node} to the pre-configured {@link AssemblyBackend}.
     *
     * @param node non-null parsed AST node
     * @param context non-null assembly generation context
     * @return generated assembly text from the backend
     * @throws IllegalArgumentException if {@code node} or {@code context} is null or invalid
     * @throws IllegalStateException if no default backend was configured or backend output is invalid
     */
    public final String generateAssembly(T node, AssemblyGenerationContext context) {
        if (assemblyBackend == null) {
            throw new IllegalStateException(
                    "AssemblyBackend is not configured for " + getClass().getSimpleName());
        }
        return generateAssembly(node, context, assemblyBackend);
    }

    /**
     * Validates inputs and delegates assembly generation for {@code node} to {@code backend}.
     *
     * @param node non-null parsed AST node
     * @param context non-null assembly generation context
     * @param backend non-null assembly backend to delegate emission to
     * @return non-blank assembly output produced by {@code backend}
     * @throws IllegalArgumentException if any argument is null or the node carries mismatched type metadata
     * @throws IllegalStateException if this parser's descriptor or backend output is invalid
     */
    public final String generateAssembly(
            T node,
            AssemblyGenerationContext context,
            AssemblyBackend backend) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (backend == null) {
            throw new IllegalArgumentException("backend must not be null");
        }

        TypeDescriptor descriptor = validatedTypeDescriptor();
        validateNodeMetadata(node, descriptor, false);

        TypeDescriptor effectiveType = node.typeDescriptor().orElse(descriptor);
        String assembly = delegateAssemblyGeneration(node, effectiveType, context, backend);
        if (assembly == null || assembly.isBlank()) {
            throw new IllegalStateException(
                    "AssemblyBackend '" + backend.targetName() + "' returned null or blank assembly output");
        }
        return assembly;
    }

    /**
     * Hook that performs the actual delegation to {@link AssemblyBackend#emitAssembly}.
     *
     * <p>Subclasses may override this hook only to enrich metadata passed to the backend,
     * never to emit assembly instructions directly.</p>
     *
     * @param node validated AST node
     * @param typeDescriptor resolved type metadata for {@code node}
     * @param context assembly generation context
     * @param backend target assembly backend
     * @return assembly text produced by {@code backend}
     */
    protected String delegateAssemblyGeneration(
            T node,
            TypeDescriptor typeDescriptor,
            AssemblyGenerationContext context,
            AssemblyBackend backend) {
        return backend.emitAssembly(node, typeDescriptor, context);
    }

    /**
     * Validates and returns this parser's {@link TypeDescriptor}.
     *
     * @return validated non-null {@link TypeDescriptor}
     * @throws IllegalStateException if {@link #getTypeDescriptor()} returns null or invalid metadata
     */
    protected final TypeDescriptor validatedTypeDescriptor() {
        TypeDescriptor descriptor = getTypeDescriptor();
        if (descriptor == null) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + ".getTypeDescriptor() must not return null");
        }
        if (descriptor.typeName() == null || descriptor.typeName().isBlank()) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " returned a TypeDescriptor with null or blank typeName");
        }
        if (descriptor.category() == null) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " returned a TypeDescriptor with null category");
        }
        if (descriptor.byteSize() < 0) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " returned a TypeDescriptor with negative byteSize");
        }
        return descriptor;
    }

    /**
     * Validates that a Java type name is non-null, non-blank, and syntactically well-formed.
     *
     * @param typeName raw Java type name
     * @return trimmed valid type name
     * @throws IllegalArgumentException if {@code typeName} is null, blank, or malformed
     */
    public static String validateTypeName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("typeName must not be null or blank");
        }
        String trimmed = typeName.trim();
        if (!VALID_TYPE_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid Java type name: '" + typeName + "'");
        }
        return trimmed;
    }

    private void validateParsedNode(T node, TypeDescriptor expectedDescriptor) {
        if (node == null) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + ".parseTypeConstruct() returned null");
        }
        validateNodeMetadata(node, expectedDescriptor, true);
    }

    private void validateNodeMetadata(
            T node,
            TypeDescriptor expectedDescriptor,
            boolean fromParseResult) {
        String kind = node.nodeKind();
        if (kind == null || kind.isBlank()) {
            if (fromParseResult) {
                throw new IllegalStateException("Parsed AST node must declare a non-blank nodeKind");
            }
            throw new IllegalArgumentException("AST node must declare a non-blank nodeKind");
        }
        Optional<TypeDescriptor> nodeType = node.typeDescriptor();
        if (nodeType == null) {
            if (fromParseResult) {
                throw new IllegalStateException("AST node.typeDescriptor() must not return null Optional");
            }
            throw new IllegalArgumentException("AST node.typeDescriptor() must not return null Optional");
        }
        if (nodeType.isPresent() && !expectedDescriptor.typeName().equals(nodeType.get().typeName())) {
            String message = "AST node type '" + nodeType.get().typeName()
                    + "' does not match parser type '" + expectedDescriptor.typeName() + "'";
            if (fromParseResult) {
                throw new IllegalStateException(message);
            }
            throw new IllegalArgumentException(message);
        }
    }
}
