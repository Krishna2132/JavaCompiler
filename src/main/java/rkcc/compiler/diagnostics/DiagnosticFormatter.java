package rkcc.compiler.diagnostics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Formats structured {@link Diagnostic} instances into human-readable compiler
 * messages with optional source-line context and caret indicators.
 */
public final class DiagnosticFormatter {

    /**
     * Formats a single {@link Diagnostic} without source text context.
     *
     * @param diagnostic non-null diagnostic to format
     * @return formatted diagnostic string
     */
    public String format(Diagnostic diagnostic) {
        return format(diagnostic, null);
    }

    /**
     * Formats a single {@link Diagnostic} with optional source-code context and caret pointer.
     *
     * @param diagnostic non-null diagnostic to format
     * @param source optional full source text (nullable; missing/empty source omits line snippet)
     * @return formatted diagnostic string
     */
    public String format(Diagnostic diagnostic, String source) {
        if (diagnostic == null) {
            throw new IllegalArgumentException("diagnostic must not be null");
        }

        StringBuilder out = new StringBuilder();
        out.append(diagnostic.severity().name())
                .append(" [").append(diagnostic.code().code()).append("] ")
                .append('(').append(diagnostic.phase().displayName()).append(')');

        diagnostic.location().ifPresent(loc -> out.append(" at line ")
                .append(loc.line())
                .append(", column ")
                .append(loc.column()));

        out.append('\n').append(diagnostic.message());
        diagnostic.details().ifPresent(details -> out.append("\nDetail: ").append(details));

        if (source != null && diagnostic.location().isPresent()) {
            Optional<String> snippet = buildSourceSnippet(source, diagnostic.location().get());
            snippet.ifPresent(s -> out.append("\n\n").append(s));
        }

        return out.toString();
    }

    /**
     * Formats a collection of diagnostics separated by blank lines.
     *
     * @param diagnostics non-null collection of diagnostics
     * @param source optional source text used for line snippets
     * @return formatted report string (empty string if {@code diagnostics} is empty)
     */
    public String formatAll(Collection<Diagnostic> diagnostics, String source) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("diagnostics must not be null");
        }
        if (diagnostics.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic == null) {
                throw new IllegalArgumentException("diagnostics must not contain null elements");
            }
            if (!first) {
                out.append("\n\n");
            }
            out.append(format(diagnostic, source));
            first = false;
        }
        return out.toString();
    }

    private Optional<String> buildSourceSnippet(String source, SourceLocation location) {
        List<String> lines = splitLinesPreservingTrailingEmpty(source);
        int zeroBasedLine = location.line() - 1;
        if (zeroBasedLine < 0 || zeroBasedLine >= lines.size()) {
            return Optional.empty();
        }

        String sourceLine = lines.get(zeroBasedLine);
        String caretLine = buildCaretLine(sourceLine, location.column());
        return Optional.of(sourceLine + "\n" + caretLine);
    }

    private static String buildCaretLine(String sourceLine, int oneBasedColumn) {
        int targetIndex = Math.max(0, oneBasedColumn - 1);
        StringBuilder caret = new StringBuilder(targetIndex + 1);

        int charPos = 0;
        int col = 0;
        while (col < targetIndex && charPos < sourceLine.length()) {
            int cp = sourceLine.codePointAt(charPos);
            if (cp == '\t') {
                caret.append('\t');
            } else {
                caret.append(' ');
            }
            charPos += Character.charCount(cp);
            col++;
        }
        while (col < targetIndex) {
            caret.append(' ');
            col++;
        }
        caret.append('^');
        return caret.toString();
    }

    private static List<String> splitLinesPreservingTrailingEmpty(String source) {
        List<String> lines = new ArrayList<>();
        int lineStart = 0;
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char ch = source.charAt(i);
            if (ch == '\r') {
                lines.add(source.substring(lineStart, i));
                if (i + 1 < len && source.charAt(i + 1) == '\n') {
                    i++;
                }
                lineStart = i + 1;
            } else if (ch == '\n') {
                lines.add(source.substring(lineStart, i));
                lineStart = i + 1;
            }
        }
        lines.add(source.substring(lineStart));
        return lines;
    }
}
