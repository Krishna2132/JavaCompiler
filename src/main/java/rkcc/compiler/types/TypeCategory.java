package rkcc.compiler.types;

/**
 * Broad classification of Java data types supported across compiler stages.
 *
 * <p>Designed to accommodate the initial integral-language subset as well as
 * future support for all Java primitives, {@code String}, arrays, and reference types.</p>
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
    UNKNOWN
}
