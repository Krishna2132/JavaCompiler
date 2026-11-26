package rkcc.compiler.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Default list-backed {@link TokenStream} providing sequential consumption,
 * arbitrary non-negative lookahead, and strict sequence validation.
 *
 * <h2>Sequence Invariants</h2>
 * <ul>
 *   <li><strong>Non-empty &amp; EOF-terminated:</strong> Empty token collections and
 *       sequences lacking a trailing {@link TokenType#EOF} token are rejected with
 *       {@link IllegalArgumentException}. An empty source file must be represented
 *       as a single-element list containing {@code Token.eof(...)}.</li>
 *   <li><strong>No duplicate or premature EOF:</strong> {@link TokenType#EOF} may
 *       appear exactly once, as the final element in the sequence.</li>
 *   <li><strong>Defensive copy:</strong> The supplied list is copied into an
 *       unmodifiable list so external caller mutations cannot corrupt stream state.</li>
 *   <li><strong>Safe repeated EOF reads:</strong> Once at EOF, {@link #peek()},
 *       {@link #peek(int)}, {@link #advance()}, and {@link #isAtEnd()} remain safe
 *       and keep the cursor positioned on the terminal EOF token.</li>
 *   <li><strong>{@code previous()} before first advance:</strong> Calling
 *       {@link #previous()} at cursor position {@code 0} throws
 *       {@link IllegalStateException}.</li>
 * </ul>
 */
public final class ListTokenStream implements TokenStream {

    private final List<Token> tokens;
    private int cursor = 0;

    /**
     * Creates a validated, defensively copied token stream.
     *
     * @param tokens non-null, non-empty list of non-null tokens ending with a single
     *               {@link TokenType#EOF} token
     * @throws IllegalArgumentException if {@code tokens} is null, contains null elements,
     *                                  is empty, lacks a trailing EOF, or has duplicate/premature EOF
     */
    public ListTokenStream(List<Token> tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("tokens must not be null");
        }
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException(
                    "Token sequence must not be empty; an EOF token is required");
        }

        List<Token> copy = new ArrayList<>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token == null) {
                throw new IllegalArgumentException("tokens must not contain null elements (index " + i + ")");
            }
            boolean isLast = (i == tokens.size() - 1);
            if (token.is(TokenType.EOF) && !isLast) {
                throw new IllegalArgumentException(
                        "Duplicate or premature EOF token at index " + i);
            }
            if (!token.is(TokenType.EOF) && isLast) {
                throw new IllegalArgumentException(
                        "Token sequence must terminate with a TokenType.EOF token");
            }
            copy.add(token);
        }
        this.tokens = List.copyOf(copy);
    }

    @Override
    public Token peek() {
        return tokens.get(cursor);
    }

    @Override
    public Token peek(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Lookahead offset must be >= 0, got: " + offset);
        }
        int eofIndex = tokens.size() - 1;
        long target = (long) cursor + offset;
        int clampedIndex = (target >= eofIndex) ? eofIndex : (int) target;
        return tokens.get(clampedIndex);
    }

    @Override
    public Token advance() {
        Token current = peek();
        if (!isAtEnd()) {
            cursor++;
        }
        return current;
    }

    @Override
    public Token previous() {
        if (cursor == 0) {
            throw new IllegalStateException(
                    "No previous token available before advancing the stream");
        }
        return tokens.get(cursor - 1);
    }

    @Override
    public boolean check(TokenType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return peek().is(type);
    }

    @Override
    public boolean match(TokenType expectedType) {
        if (expectedType == null) {
            throw new IllegalArgumentException("expectedType must not be null");
        }
        if (isAtEnd() || !check(expectedType)) {
            return false;
        }
        advance();
        return true;
    }

    @Override
    public boolean match(TokenType... types) {
        if (types == null) {
            throw new IllegalArgumentException("types must not be null");
        }
        for (int i = 0; i < types.length; i++) {
            if (types[i] == null) {
                throw new IllegalArgumentException("types must not contain null elements (index " + i + ")");
            }
        }
        if (isAtEnd()) {
            return false;
        }
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    @Override
    public Token consume(TokenType expectedType, String message) {
        if (expectedType == null) {
            throw new IllegalArgumentException("expectedType must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank");
        }
        if (!check(expectedType)) {
            throw new UnexpectedTokenException(expectedType, peek(), message);
        }
        if (expectedType == TokenType.EOF) {
            return peek();
        }
        return advance();
    }

    @Override
    public boolean isAtEnd() {
        return peek().is(TokenType.EOF);
    }

    @Override
    public int position() {
        return cursor;
    }
}
