package rkcc.compiler.ast;

import java.util.Optional;
import rkcc.compiler.types.TypeDescriptor;

/**
 * Minimal base contract for Abstract Syntax Tree nodes produced by the parser
 * and {@code AbstractTypeParser} extensions.
 *
 * <p>Concrete AST node classes and traversal visitors belong to the parser/AST milestone.</p>
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
}
