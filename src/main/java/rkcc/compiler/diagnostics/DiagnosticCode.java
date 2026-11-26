package rkcc.compiler.diagnostics;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stable diagnostic code registry shared across all compiler modules.
 *
 * <p>Codes for upcoming parser, semantic, type, and x86 code-generation stages
 * are reserved here so team modules share consistent error identifiers.</p>
 */
public enum DiagnosticCode {

    // Lexical errors (LEX001 - LEX006)
    LEX_UNEXPECTED_CHARACTER("LEX001", CompilerPhase.LEXICAL_ANALYSIS, "Unexpected character"),
    LEX_INVALID_NUMERIC_LITERAL("LEX002", CompilerPhase.LEXICAL_ANALYSIS, "Invalid numeric literal"),
    LEX_UNTERMINATED_STRING("LEX003", CompilerPhase.LEXICAL_ANALYSIS, "Unterminated string literal"),
    LEX_INVALID_CHARACTER_LITERAL("LEX004", CompilerPhase.LEXICAL_ANALYSIS, "Invalid character literal"),
    LEX_INVALID_ESCAPE_SEQUENCE("LEX005", CompilerPhase.LEXICAL_ANALYSIS, "Invalid escape sequence"),
    LEX_UNTERMINATED_BLOCK_COMMENT("LEX006", CompilerPhase.LEXICAL_ANALYSIS, "Unterminated block comment"),

    // Syntax errors (SYN001 - SYN004)
    SYN_UNEXPECTED_TOKEN("SYN001", CompilerPhase.SYNTAX_ANALYSIS, "Unexpected token"),
    SYN_EXPECTED_TOKEN_MISSING("SYN002", CompilerPhase.SYNTAX_ANALYSIS, "Expected token missing"),
    SYN_UNEXPECTED_END_OF_INPUT("SYN003", CompilerPhase.SYNTAX_ANALYSIS, "Unexpected end of input"),
    SYN_INVALID_EXPRESSION("SYN004", CompilerPhase.SYNTAX_ANALYSIS, "Invalid expression"),

    // Semantic and type errors (SEM001 - SEM006)
    SEM_UNDEFINED_VARIABLE("SEM001", CompilerPhase.SEMANTIC_ANALYSIS, "Undefined variable"),
    SEM_DUPLICATE_DECLARATION("SEM002", CompilerPhase.SEMANTIC_ANALYSIS, "Duplicate declaration"),
    TYP_TYPE_MISMATCH("SEM003", CompilerPhase.TYPE_CHECKING, "Type mismatch"),
    TYP_INVALID_CONVERSION("SEM004", CompilerPhase.TYPE_CHECKING, "Invalid type conversion"),
    TYP_UNSUPPORTED_TYPE("SEM005", CompilerPhase.TYPE_CHECKING, "Unsupported type"),
    TYP_INTEGER_OUT_OF_RANGE("SEM006", CompilerPhase.TYPE_CHECKING, "Integer literal out of range"),

    // Code generation and internal errors (GEN001 - GEN003, INT001)
    GEN_UNSUPPORTED_OPERATION("GEN001", CompilerPhase.CODE_GENERATION, "Unsupported operation"),
    GEN_INVALID_ASSEMBLY_OPERAND("GEN002", CompilerPhase.CODE_GENERATION, "Invalid assembly operand"),
    GEN_UNSUPPORTED_TARGET("GEN003", CompilerPhase.CODE_GENERATION, "Unsupported target"),
    INT_INTERNAL_COMPILER_ERROR("INT001", CompilerPhase.INTERNAL, "Internal compiler error");

    private static final Map<String, DiagnosticCode> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(DiagnosticCode::code, Function.identity()));

    private final String code;
    private final CompilerPhase defaultPhase;
    private final String defaultDescription;

    DiagnosticCode(String code, CompilerPhase defaultPhase, String defaultDescription) {
        this.code = code;
        this.defaultPhase = defaultPhase;
        this.defaultDescription = defaultDescription;
    }

    /**
     * Returns the stable external identifier (for example, {@code "LEX001"}).
     */
    public String code() {
        return code;
    }

    /**
     * Returns the primary compiler phase associated with this code.
     */
    public CompilerPhase defaultPhase() {
        return defaultPhase;
    }

    /**
     * Returns a concise default summary for this diagnostic code.
     */
    public String defaultDescription() {
        return defaultDescription;
    }

    /**
     * Looks up a {@link DiagnosticCode} by its external code string.
     */
    public static Optional<DiagnosticCode> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
