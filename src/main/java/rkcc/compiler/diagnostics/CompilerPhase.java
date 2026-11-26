package rkcc.compiler.diagnostics;

/**
 * Compiler pipeline phases that emit diagnostics.
 */
public enum CompilerPhase {

    LEXICAL_ANALYSIS("Lexical Analysis"),
    SYNTAX_ANALYSIS("Syntax Analysis"),
    SEMANTIC_ANALYSIS("Semantic Analysis"),
    TYPE_CHECKING("Type Checking"),
    CODE_GENERATION("Code Generation"),
    INTERNAL("Internal");

    private final String displayName;

    CompilerPhase(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns a human-readable phase label for diagnostic formatting.
     */
    public String displayName() {
        return displayName;
    }
}
