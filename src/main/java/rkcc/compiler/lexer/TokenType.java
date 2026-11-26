package rkcc.compiler.lexer;

import java.util.Map;
import java.util.Optional;

/**
 * Strongly typed enumeration of Java lexical token categories supported by the
 * RKCC JavaCompiler token model.
 *
 * <p>Note: {@code String} is a reference type identifier ({@link #IDENTIFIER}),
 * not a Java primitive keyword.</p>
 */
public enum TokenType {

    // Java primitive type keywords (plus void return type keyword)
    BYTE("byte", Category.PRIMITIVE_TYPE),
    SHORT("short", Category.PRIMITIVE_TYPE),
    INT("int", Category.PRIMITIVE_TYPE),
    LONG("long", Category.PRIMITIVE_TYPE),
    FLOAT("float", Category.PRIMITIVE_TYPE),
    DOUBLE("double", Category.PRIMITIVE_TYPE),
    CHAR("char", Category.PRIMITIVE_TYPE),
    BOOLEAN("boolean", Category.PRIMITIVE_TYPE),
    VOID("void", Category.PRIMITIVE_TYPE),

    // Identifiers and literals
    IDENTIFIER(null, Category.IDENTIFIER),
    INTEGER_LITERAL(null, Category.LITERAL),
    FLOATING_POINT_LITERAL(null, Category.LITERAL),
    CHARACTER_LITERAL(null, Category.LITERAL),
    STRING_LITERAL(null, Category.LITERAL),
    TRUE("true", Category.LITERAL),
    FALSE("false", Category.LITERAL),
    NULL("null", Category.LITERAL),

    // Arithmetic operators
    PLUS("+", Category.ARITHMETIC_OPERATOR),
    MINUS("-", Category.ARITHMETIC_OPERATOR),
    STAR("*", Category.ARITHMETIC_OPERATOR),
    SLASH("/", Category.ARITHMETIC_OPERATOR),
    PERCENT("%", Category.ARITHMETIC_OPERATOR),

    // Assignment operators
    ASSIGN("=", Category.ASSIGNMENT_OPERATOR),
    PLUS_ASSIGN("+=", Category.ASSIGNMENT_OPERATOR),
    MINUS_ASSIGN("-=", Category.ASSIGNMENT_OPERATOR),
    STAR_ASSIGN("*=", Category.ASSIGNMENT_OPERATOR),
    SLASH_ASSIGN("/=", Category.ASSIGNMENT_OPERATOR),
    PERCENT_ASSIGN("%=", Category.ASSIGNMENT_OPERATOR),
    BITWISE_AND_ASSIGN("&=", Category.ASSIGNMENT_OPERATOR),
    BITWISE_OR_ASSIGN("|=", Category.ASSIGNMENT_OPERATOR),
    BITWISE_XOR_ASSIGN("^=", Category.ASSIGNMENT_OPERATOR),
    LEFT_SHIFT_ASSIGN("<<=", Category.ASSIGNMENT_OPERATOR),
    RIGHT_SHIFT_ASSIGN(">>=", Category.ASSIGNMENT_OPERATOR),
    UNSIGNED_RIGHT_SHIFT_ASSIGN(">>>=", Category.ASSIGNMENT_OPERATOR),

    // Bitwise operators
    BITWISE_AND("&", Category.BITWISE_OPERATOR),
    BITWISE_OR("|", Category.BITWISE_OPERATOR),
    BITWISE_XOR("^", Category.BITWISE_OPERATOR),
    BITWISE_COMPLEMENT("~", Category.BITWISE_OPERATOR),
    LEFT_SHIFT("<<", Category.BITWISE_OPERATOR),
    RIGHT_SHIFT(">>", Category.BITWISE_OPERATOR),
    UNSIGNED_RIGHT_SHIFT(">>>", Category.BITWISE_OPERATOR),

    // Comparison and logical operators
    EQUAL_EQUAL("==", Category.COMPARISON_OPERATOR),
    NOT_EQUAL("!=", Category.COMPARISON_OPERATOR),
    LESS_THAN("<", Category.COMPARISON_OPERATOR),
    GREATER_THAN(">", Category.COMPARISON_OPERATOR),
    LESS_THAN_OR_EQUAL("<=", Category.COMPARISON_OPERATOR),
    GREATER_THAN_OR_EQUAL(">=", Category.COMPARISON_OPERATOR),
    LOGICAL_AND("&&", Category.LOGICAL_OPERATOR),
    LOGICAL_OR("||", Category.LOGICAL_OPERATOR),
    LOGICAL_NOT("!", Category.LOGICAL_OPERATOR),

    // Increment and decrement operators
    INCREMENT("++", Category.UNARY_OPERATOR),
    DECREMENT("--", Category.UNARY_OPERATOR),

    // Delimiters and punctuation
    LEFT_PAREN("(", Category.PUNCTUATION),
    RIGHT_PAREN(")", Category.PUNCTUATION),
    LEFT_BRACE("{", Category.PUNCTUATION),
    RIGHT_BRACE("}", Category.PUNCTUATION),
    LEFT_BRACKET("[", Category.PUNCTUATION),
    RIGHT_BRACKET("]", Category.PUNCTUATION),
    SEMICOLON(";", Category.PUNCTUATION),
    COMMA(",", Category.PUNCTUATION),
    DOT(".", Category.PUNCTUATION),
    COLON(":", Category.PUNCTUATION),
    QUESTION_MARK("?", Category.PUNCTUATION),

    // End of file sentinel
    EOF("", Category.EOF);

    private static final Map<String, TokenType> KEYWORD_LOOKUP = Map.ofEntries(
            Map.entry("byte", BYTE),
            Map.entry("short", SHORT),
            Map.entry("int", INT),
            Map.entry("long", LONG),
            Map.entry("float", FLOAT),
            Map.entry("double", DOUBLE),
            Map.entry("char", CHAR),
            Map.entry("boolean", BOOLEAN),
            Map.entry("void", VOID),
            Map.entry("true", TRUE),
            Map.entry("false", FALSE),
            Map.entry("null", NULL)
    );

    private final String fixedLexeme;
    private final Category category;

    TokenType(String fixedLexeme, Category category) {
        this.fixedLexeme = fixedLexeme;
        this.category = category;
    }

    /**
     * Returns the fixed lexeme associated with this token type, or empty for
     * variable-lexeme tokens (identifiers and numeric/char/string literals).
     */
    public Optional<String> fixedLexeme() {
        return Optional.ofNullable(fixedLexeme);
    }

    /**
     * Returns the structural category grouping for this token type.
     */
    public Category category() {
        return category;
    }

    /**
     * Indicates whether this token represents a Java primitive type keyword
     * (including {@code void}).
     */
    public boolean isPrimitiveTypeKeyword() {
        return category == Category.PRIMITIVE_TYPE;
    }

    /**
     * Indicates whether this token represents a literal value ({@code true},
     * {@code false}, {@code null}, or numeric/char/string literal).
     */
    public boolean isLiteral() {
        return category == Category.LITERAL;
    }

    /**
     * Indicates whether this token is an arithmetic operator.
     */
    public boolean isArithmeticOperator() {
        return category == Category.ARITHMETIC_OPERATOR;
    }

    /**
     * Indicates whether this token is a simple or compound assignment operator.
     */
    public boolean isAssignmentOperator() {
        return category == Category.ASSIGNMENT_OPERATOR;
    }

    /**
     * Indicates whether this token is a bitwise or shift operator.
     */
    public boolean isBitwiseOperator() {
        return category == Category.BITWISE_OPERATOR;
    }

    /**
     * Indicates whether this token is a relational or equality comparison operator.
     */
    public boolean isComparisonOperator() {
        return category == Category.COMPARISON_OPERATOR;
    }

    /**
     * Indicates whether this token is a logical boolean operator.
     */
    public boolean isLogicalOperator() {
        return category == Category.LOGICAL_OPERATOR;
    }

    /**
     * Looks up a keyword {@link TokenType} for the given lexeme.
     *
     * <p>{@code "String"} intentionally returns {@link Optional#empty()} because
     * {@code String} is a reference type identifier, not a primitive keyword.</p>
     *
     * @param lexeme candidate keyword lexeme
     * @return matching {@link TokenType} if {@code lexeme} is a supported keyword
     */
    public static Optional<TokenType> fromKeyword(String lexeme) {
        if (lexeme == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(KEYWORD_LOOKUP.get(lexeme));
    }

    /**
     * Coarse grouping of token types for parser and diagnostic classification.
     */
    public enum Category {
        PRIMITIVE_TYPE,
        IDENTIFIER,
        LITERAL,
        ARITHMETIC_OPERATOR,
        ASSIGNMENT_OPERATOR,
        BITWISE_OPERATOR,
        COMPARISON_OPERATOR,
        LOGICAL_OPERATOR,
        UNARY_OPERATOR,
        PUNCTUATION,
        EOF
    }
}
