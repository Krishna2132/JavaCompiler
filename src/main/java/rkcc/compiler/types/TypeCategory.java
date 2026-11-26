package rkcc.compiler.types;

/**
 * Broad classification of Java data types supported across compiler stages.
 *
 * <p>Designed to accommodate the initial integral-language subset as well as
 * future support for all Java primitives, {@code String}, arrays, reference
 * types, and the {@code void} return type.</p>
 */
public enum TypeCategory {
    INTEGRAL,
    FLOATING_POINT,
    BOOLEAN,
    CHAR,
    STRING,
    ARRAY,
    REFERENCE,
    VOID,
    UNKNOWN;

    /**
     * Returns {@code true} if this category represents a Java primitive category.
     */
    public boolean isPrimitiveCategory() {
        return this == INTEGRAL
                || this == FLOATING_POINT
                || this == BOOLEAN
                || this == CHAR;
    }

    /**
     * Returns {@code true} if this category represents a Java reference type
     * ({@code String}, arrays, classes, interfaces, or other reference types).
     */
    public boolean isReferenceCategory() {
        return this == STRING || this == ARRAY || this == REFERENCE;
    }

    /**
     * Returns {@code true} if this category represents the special {@code void} return type.
     */
    public boolean isVoid() {
        return this == VOID;
    }
}
