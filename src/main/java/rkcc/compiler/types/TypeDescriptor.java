package rkcc.compiler.types;

/**
 * Shared metadata contract describing a Java data type across parsing,
 * semantic validation, and assembly generation.
 */
public interface TypeDescriptor {

    /**
     * Returns the canonical Java type name (for example, {@code "int"},
     * {@code "boolean"}, {@code "String"}, or {@code "int[]"}).
     *
     * @return non-null canonical type name
     */
    String typeName();

    /**
     * Returns the broad {@link TypeCategory} of this type.
     *
     * @return non-null type category
     */
    TypeCategory category();

    /**
     * Returns the target storage width in bytes, or {@code 0} for unsized types.
     *
     * @return non-negative storage size in bytes
     */
    int byteSize();

    /**
     * Indicates whether values of this type use signed representation.
     *
     * @return {@code true} if signed, {@code false} otherwise
     */
    boolean isSigned();

    /**
     * Indicates whether this type is one of Java's primitive types.
     *
     * @return {@code true} if primitive, {@code false} for reference/array/other types
     */
    boolean isPrimitive();

    /**
     * Determines whether a value of this type is assignable to {@code target}.
     *
     * @param target destination type descriptor
     * @return {@code true} if assignment is valid under Java type rules
     */
    boolean isAssignableTo(TypeDescriptor target);
}
