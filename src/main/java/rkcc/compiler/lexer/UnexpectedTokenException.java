package rkcc.compiler.lexer;

import rkcc.compiler.diagnostics.CompilerException;
import rkcc.compiler.diagnostics.CompilerPhase;
import rkcc.compiler.diagnostics.Diagnostic;
import rkcc.compiler.diagnostics.DiagnosticCode;
import rkcc.compiler.diagnostics.DiagnosticSeverity;
import rkcc.compiler.diagnostics.SourceLocation;

/**
 * Unchecked exception thrown by {@link TokenStream#consume(TokenType, String)}
 * when the current token does not match the expected {@link TokenType}.
 *
 * <p>Extends {@link CompilerException} so token-stream errors carry a structured
 * {@link Diagnostic} with {@link DiagnosticCode}, expected token type, actual
 * token type/lexeme, and {@link SourceLocation}.</p>
 */
public class UnexpectedTokenException extends CompilerException {

    private final TokenType expectedType;
    private final Token actualToken;

    /**
     * Constructs an {@code UnexpectedTokenException} inferring the appropriate
     * syntax {@link DiagnosticCode} from whether the actual token is {@link TokenType#EOF}.
     */
    public UnexpectedTokenException(
            TokenType expectedType,
            Token actualToken,
            String message) {
        this(defaultCodeFor(actualToken), expectedType, actualToken, message, null);
    }

    /**
     * Constructs an {@code UnexpectedTokenException} with an explicit {@link DiagnosticCode}.
     */
    public UnexpectedTokenException(
            DiagnosticCode code,
            TokenType expectedType,
            Token actualToken,
            String message) {
        this(code, expectedType, actualToken, message, null);
    }

    /**
     * Constructs an {@code UnexpectedTokenException} with an explicit {@link DiagnosticCode}
     * and underlying cause.
     */
    public UnexpectedTokenException(
            DiagnosticCode code,
            TokenType expectedType,
            Token actualToken,
            String message,
            Throwable cause) {
        super(buildDiagnostic(code, expectedType, actualToken, message), cause);
        this.expectedType = expectedType;
        this.actualToken = actualToken;
    }

    /**
     * Returns the {@link TokenType} expected by the caller.
     */
    public TokenType getExpectedType() {
        return expectedType;
    }

    /**
     * Returns the actual {@link Token} found at the current stream position.
     */
    public Token getActualToken() {
        return actualToken;
    }

    /**
     * Returns the {@link SourceLocation} of the unexpected token.
     */
    public SourceLocation getLocation() {
        return actualToken.location();
    }

    private static DiagnosticCode defaultCodeFor(Token actualToken) {
        if (actualToken != null && actualToken.is(TokenType.EOF)) {
            return DiagnosticCode.SYN_UNEXPECTED_END_OF_INPUT;
        }
        return DiagnosticCode.SYN_UNEXPECTED_TOKEN;
    }

    private static Diagnostic buildDiagnostic(
            DiagnosticCode code,
            TokenType expectedType,
            Token actualToken,
            String message) {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        if (expectedType == null) {
            throw new IllegalArgumentException("expectedType must not be null");
        }
        if (actualToken == null) {
            throw new IllegalArgumentException("actualToken must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank");
        }
        String formattedMessage = message.trim()
                + " [expected=" + expectedType
                + ", actual=" + actualToken.type()
                + " ('" + actualToken.lexeme() + "')"
                + ", location=" + actualToken.location().formatCompact()
                + "]";
        return new Diagnostic(
                code,
                DiagnosticSeverity.ERROR,
                CompilerPhase.SYNTAX_ANALYSIS,
                formattedMessage,
                actualToken.location());
    }
}
