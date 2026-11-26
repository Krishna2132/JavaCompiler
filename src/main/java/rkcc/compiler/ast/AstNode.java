package rkcc.compiler.ast;

import java.util.Optional;
import rkcc.compiler.diagnostics.SourceLocation;
import rkcc.compiler.types.TypeDescriptor;

/**
 * Minimal base contract for Abstract Syntax Tree nodes produced by the parser
 * and {@code AbstractTypeParser} extensions.
 *
 * <p>Concrete AST node classes and traversal visitors belong to Jatin's parser/AST module.</p>
 */
public interface AstNode {

    /**
     * Returns a stable structural identifier for this AST node (for example,
     * {@code "IntLiteral"} or {@code "VariableDeclaration"}).
     *
     * @return non-blank AST node kind name
     */
    String nodeKind();

    /**
     * Returns the type metadata associated with this node if available at the current stage.
     *
     * @return an {@link Optional} containing the node's {@link TypeDescriptor}, or empty if untyped
     */
    Optional<TypeDescriptor> typeDescriptor();

    /**
     * Returns the starting {@link SourceLocation} of this AST node when tracked.
     *
     * <p>Default implementation returns {@link Optional#empty()} for backward compatibility
     * with minimal test doubles; concrete AST nodes should return their source location so
     * semantic and codegen stages can emit accurate diagnostics.</p>
     *
     * @return an {@link Optional} containing the node's starting location, or empty if unavailable
     */
    default Optional<SourceLocation> location() {
        return Optional.empty();
    }
}
