package rkcc.compiler.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import rkcc.compiler.diagnostics.CompilerException;
import rkcc.compiler.diagnostics.CompilerPhase;
import rkcc.compiler.diagnostics.Diagnostic;
import rkcc.compiler.diagnostics.DiagnosticCode;
import rkcc.compiler.diagnostics.DiagnosticCollector;
import rkcc.compiler.diagnostics.DiagnosticSeverity;
import rkcc.compiler.diagnostics.SourceLocation;
import rkcc.compiler.lexer.Token;
import rkcc.compiler.lexer.TokenType;

/**
 * Centralized, session-scoped registry mapping canonical Java type names to
 * {@link AbstractTypeParser} implementations.
 *
 * <p>Placed in {@code rkcc.compiler.core} alongside {@link AbstractTypeParser} so
 * {@code rkcc.compiler.types} remains dependency-free and acyclic. Supports atomic
 * single-type and multi-type parser registrations, case-sensitive lookup,
 * token-based type resolution for supported keywords and {@code String}, and
 * structured diagnostic reporting without hardcoding concrete type parsers.</p>
 */
public final class TypeRegistry {

    private final Map<String, AbstractTypeParser<?>> parsersByType = new LinkedHashMap<>();

    /**
     * Registers {@code parser} for every Java type name advertised by
     * {@link AbstractTypeParser#getSupportedTypeNames()}.
     *
     * <p>All type names are validated and checked for duplicates or conflicts
     * before modifying the registry so failures never leave partial state.</p>
     *
     * @param parser non-null type parser implementation
     * @throws IllegalArgumentException if {@code parser} is {@code null}
     * @throws CompilerException if {@code parser} exposes invalid metadata or conflicts
     *                           with an existing registration
     */
    public void register(AbstractTypeParser<?> parser) {
        if (parser == null) {
            throw new IllegalArgumentException("parser must not be null");
        }
        Set<String> supportedNames;
        try {
            supportedNames = parser.validatedSupportedTypeNames();
        } catch (IllegalStateException ex) {
            throw invalidParserException(
                    "Invalid type parser configuration for " + parser.getClass().getSimpleName(),
                    ex);
        }
        register(supportedNames, parser);
    }

    /**
     * Registers {@code parser} for a single explicit Java {@code typeName}.
     *
     * @param typeName canonical case-sensitive Java type name
     * @param parser non-null type parser supporting {@code typeName}
     * @throws IllegalArgumentException if {@code typeName} or {@code parser} is null/invalid
     * @throws CompilerException if {@code parser} does not support {@code typeName} or
     *                           conflicts with an existing registration
     */
    public void register(String typeName, AbstractTypeParser<?> parser) {
        String normalized = AbstractTypeParser.validateTypeName(typeName);
        if (parser == null) {
            throw new IllegalArgumentException("parser must not be null");
        }
        register(List.of(normalized), parser);
    }

    /**
     * Atomically registers {@code parser} for all supplied {@code typeNames}.
     *
     * @param typeNames non-null, non-empty collection of valid Java type names
     * @param parser non-null type parser supporting every name in {@code typeNames}
     * @throws IllegalArgumentException if arguments or type names are null/empty/malformed
     * @throws CompilerException on duplicate, conflicting, or incompatible parser registration
     */
    public void register(Collection<String> typeNames, AbstractTypeParser<?> parser) {
        if (typeNames == null || typeNames.isEmpty()) {
            throw new IllegalArgumentException("typeNames must not be null or empty");
        }
        if (parser == null) {
            throw new IllegalArgumentException("parser must not be null");
        }

        try {
            parser.validatedTypeDescriptor();
        } catch (IllegalStateException ex) {
            throw invalidParserException(
                    "Invalid type descriptor for parser " + parser.getClass().getSimpleName(),
                    ex);
        }

        List<String> validatedNames = new ArrayList<>(typeNames.size());
        Set<String> seenInBatch = new LinkedHashSet<>();

        for (String rawName : typeNames) {
            String normalized = AbstractTypeParser.validateTypeName(rawName);
            if (!seenInBatch.add(normalized)) {
                throw new CompilerException(new Diagnostic(
                        DiagnosticCode.INT_INTERNAL_COMPILER_ERROR,
                        DiagnosticSeverity.ERROR,
                        CompilerPhase.INTERNAL,
                        "Duplicate type '" + normalized + "' in batch registration for "
                                + parser.getClass().getSimpleName(),
                        null));
            }
            if (!parser.supports(normalized)) {
                throw invalidParserException(
                        "Parser " + parser.getClass().getSimpleName()
                                + " does not support declared type '" + normalized + "'",
                        null);
            }
            AbstractTypeParser<?> existing = parsersByType.get(normalized);
            if (existing != null) {
                String message = (existing == parser)
                        ? "Duplicate registration for type '" + normalized + "' with parser "
                                + parser.getClass().getSimpleName()
                        : "Conflicting parser registration for type '" + normalized + "': already mapped to "
                                + existing.getClass().getSimpleName() + ", cannot register "
                                + parser.getClass().getSimpleName();
                throw new CompilerException(new Diagnostic(
                        DiagnosticCode.INT_INTERNAL_COMPILER_ERROR,
                        DiagnosticSeverity.ERROR,
                        CompilerPhase.INTERNAL,
                        message,
                        null));
            }
            validatedNames.add(normalized);
        }

        for (String name : validatedNames) {
            parsersByType.put(name, parser);
        }
    }

    /**
     * Registers {@code parser} and records any internal configuration diagnostic into
     * {@code diagnostics} before rethrowing {@link CompilerException}.
     *
     * @param parser non-null parser to register
     * @param diagnostics non-null diagnostic collector
     */
    public void register(AbstractTypeParser<?> parser, DiagnosticCollector diagnostics) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("diagnostics must not be null");
        }
        try {
            register(parser);
        } catch (CompilerException ex) {
            diagnostics.report(ex.getDiagnostic());
            throw ex;
        }
    }

    /**
     * Resolves the registered {@link AbstractTypeParser} for {@code typeName}.
     *
     * @param typeName case-sensitive Java type name
     * @return an {@link Optional} containing the matching parser, or empty if unregistered
     * @throws IllegalArgumentException if {@code typeName} is null, blank, or syntactically invalid
     */
    public Optional<AbstractTypeParser<?>> resolve(String typeName) {
        String normalized = AbstractTypeParser.validateTypeName(typeName);
        return Optional.ofNullable(parsersByType.get(normalized));
    }

    /**
     * Resolves a registered parser for {@code typeName}, recording a
     * {@link DiagnosticCode#TYP_UNSUPPORTED_TYPE} diagnostic if unregistered.
     *
     * @param typeName case-sensitive Java type name
     * @param diagnostics non-null diagnostic collector
     * @return matching parser, or empty if unregistered
     */
    public Optional<AbstractTypeParser<?>> resolve(
            String typeName,
            DiagnosticCollector diagnostics) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("diagnostics must not be null");
        }
        String normalized = AbstractTypeParser.validateTypeName(typeName);
        Optional<AbstractTypeParser<?>> resolved = resolve(normalized);
        if (resolved.isEmpty()) {
            diagnostics.reportError(
                    DiagnosticCode.TYP_UNSUPPORTED_TYPE,
                    "Unsupported or unregistered Java type '" + normalized + "'",
                    null);
        }
        return resolved;
    }

    /**
     * Resolves a registered parser for {@code typeName} or throws {@link CompilerException}
     * with {@link DiagnosticCode#TYP_UNSUPPORTED_TYPE}.
     *
     * @param typeName case-sensitive Java type name
     * @return non-null registered parser
     */
    public AbstractTypeParser<?> resolveRequired(String typeName) {
        String normalized = AbstractTypeParser.validateTypeName(typeName);
        return resolve(normalized).orElseThrow(() -> new CompilerException(Diagnostic.error(
                DiagnosticCode.TYP_UNSUPPORTED_TYPE,
                "Unsupported or unregistered Java type '" + normalized + "'",
                null)));
    }

    /**
     * Resolves a registered parser from a lexical {@link Token}.
     *
     * <p>Only Java primitive keywords ({@code byte}..{@code void}) and the
     * reference-type identifier {@code "String"} are mapped to type lookups;
     * arbitrary identifiers and non-type tokens return {@link Optional#empty()}.</p>
     *
     * @param token non-null lexical token
     * @return matching registered parser, or empty if {@code token} is not a recognized
     *         type token or no parser is registered for it
     * @throws IllegalArgumentException if {@code token} is {@code null}
     */
    public Optional<AbstractTypeParser<?>> resolve(Token token) {
        if (token == null) {
            throw new IllegalArgumentException("token must not be null");
        }
        Optional<String> candidateTypeName = extractSupportedTypeTokenName(token);
        if (candidateTypeName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(parsersByType.get(candidateTypeName.get()));
    }

    /**
     * Resolves a registered parser from {@code token} and reports a structured
     * {@link DiagnosticCode#TYP_UNSUPPORTED_TYPE} error carrying {@link Token#location()}
     * when no registered parser matches.
     *
     * @param token non-null lexical token
     * @param diagnostics non-null diagnostic collector
     * @return matching registered parser, or empty if unsupported/unregistered
     */
    public Optional<AbstractTypeParser<?>> resolve(
            Token token,
            DiagnosticCollector diagnostics) {
        if (token == null) {
            throw new IllegalArgumentException("token must not be null");
        }
        if (diagnostics == null) {
            throw new IllegalArgumentException("diagnostics must not be null");
        }
        Optional<AbstractTypeParser<?>> resolved = resolve(token);
        if (resolved.isEmpty()) {
            SourceLocation location = token.location();
            String label = token.is(TokenType.EOF) ? "<EOF>" : token.lexeme();
            diagnostics.reportError(
                    DiagnosticCode.TYP_UNSUPPORTED_TYPE,
                    "Unsupported or unregistered type token '" + label + "' (" + token.type() + ")",
                    location);
        }
        return resolved;
    }

    /**
     * Checks whether a parser is currently registered for {@code typeName}.
     *
     * @param typeName case-sensitive Java type name
     * @return {@code true} if a parser is registered
     * @throws IllegalArgumentException if {@code typeName} is null, blank, or invalid
     */
    public boolean supports(String typeName) {
        String normalized = AbstractTypeParser.validateTypeName(typeName);
        return parsersByType.containsKey(normalized);
    }

    /**
     * Checks whether a parser is currently registered for the given {@link Token}.
     *
     * @param token non-null token
     * @return {@code true} if {@link #resolve(Token)} finds a registered parser
     */
    public boolean supports(Token token) {
        return resolve(token).isPresent();
    }

    /**
     * Removes the parser mapping for {@code typeName}, if present.
     *
     * @param typeName case-sensitive Java type name
     * @return removed parser if present, or empty otherwise
     * @throws IllegalArgumentException if {@code typeName} is null, blank, or invalid
     */
    public Optional<AbstractTypeParser<?>> unregister(String typeName) {
        String normalized = AbstractTypeParser.validateTypeName(typeName);
        return Optional.ofNullable(parsersByType.remove(normalized));
    }

    /**
     * Returns an unmodifiable snapshot of all registered Java type names in
     * deterministic registration order.
     */
    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(parsersByType.keySet()));
    }

    /**
     * Returns an unmodifiable snapshot of distinct registered {@link AbstractTypeParser}
     * instances in deterministic registration order.
     */
    public Set<AbstractTypeParser<?>> getRegisteredParsers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(parsersByType.values()));
    }

    /**
     * Returns the number of registered type-name mappings.
     */
    public int size() {
        return parsersByType.size();
    }

    /**
     * Returns {@code true} if no type parsers are registered.
     */
    public boolean isEmpty() {
        return parsersByType.isEmpty();
    }

    private static Optional<String> extractSupportedTypeTokenName(Token token) {
        if (token.isPrimitiveTypeKeyword()) {
            return Optional.of(token.lexeme());
        }
        if (token.is(TokenType.IDENTIFIER) && "String".equals(token.lexeme())) {
            return Optional.of("String");
        }
        return Optional.empty();
    }

    private static CompilerException invalidParserException(String message, Throwable cause) {
        Diagnostic diagnostic = new Diagnostic(
                DiagnosticCode.INT_INTERNAL_COMPILER_ERROR,
                DiagnosticSeverity.ERROR,
                CompilerPhase.INTERNAL,
                message,
                null);
        return (cause == null)
                ? new CompilerException(diagnostic)
                : new CompilerException(diagnostic, cause);
    }
}
