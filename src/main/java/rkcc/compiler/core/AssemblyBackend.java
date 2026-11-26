package rkcc.compiler.core;

import rkcc.compiler.ast.AstNode;
import rkcc.compiler.types.TypeDescriptor;

/**
 * Delegation contract for assembly code-generation backends.
 *
 * <p>Defined in {@code rkcc.compiler.core} to preserve acyclic module dependencies:
 * {@link AbstractTypeParser} delegates to this interface, while concrete backend
 * implementations reside in {@code rkcc.compiler.codegen}.</p>
 */
public interface AssemblyBackend {

    /**
     * Returns the identifier of the target assembly platform or syntax handled by this backend.
     *
     * @return non-blank target name
     */
    String targetName();

    /**
     * Generates target assembly output for the supplied AST node and resolved type metadata.
     *
     * @param node structured AST node to emit assembly for
     * @param typeDescriptor canonical type metadata describing the node's Java type
     * @param context generation context carrying target metadata
     * @return generated assembly text produced by the backend
     */
    String emitAssembly(AstNode node, TypeDescriptor typeDescriptor, AssemblyGenerationContext context);
}
