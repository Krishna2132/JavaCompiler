package rkcc.compiler.diagnostics;

/**
 * Immutable 1-based line/column and 0-based character offset source location.
 *
 * <p>Shared across lexical tokens, AST nodes, semantic checks, and compiler
 * diagnostics so every error and warning maps cleanly back to source input.</p>
 *
 * @param line 1-based line number ({@code >= 1})
 * @param column 1-based column number ({@code >= 1})
 * @param offset 0-based character index from the start of the source input ({@code >= 0})
 */
public record SourceLocation(int line, int column, int offset) {

    /**
     * Canonical start-of-file source location (line 1, column 1, offset 0).
     */
    public static final SourceLocation START = new SourceLocation(1, 1, 0);

    public SourceLocation {
        if (line < 1) {
            throw new IllegalArgumentException("line must be >= 1, got: " + line);
        }
        if (column < 1) {
            throw new IllegalArgumentException("column must be >= 1, got: " + column);
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0, got: " + offset);
        }
    }

    /**
     * Returns a compact {@code line:column} representation suitable for diagnostic headers.
     *
     * @return formatted {@code line:column} string
     */
    public String formatCompact() {
        return line + ":" + column;
    }

    @Override
    public String toString() {
        return "SourceLocation[line=" + line + ", column=" + column + ", offset=" + offset + "]";
    }
}
