package rkcc.compiler.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import rkcc.compiler.ast.AstNode;
import rkcc.compiler.core.AbstractTypeParser;
import rkcc.compiler.diagnostics.SourceLocation;
import rkcc.compiler.types.TypeCategory;
import rkcc.compiler.types.TypeDescriptor;

/**
 * Comprehensive unit and integration-example tests for {@link ListTokenStream}
 * and {@link UnexpectedTokenException}.
 */
class ListTokenStreamTest {

    @Test
    void consumesTokensSequentiallyAndTracksPositionAndPrevious() {
        Token t1 = new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0));
        Token t2 = new Token(TokenType.IDENTIFIER, "x", new SourceLocation(1, 5, 4));
        Token t3 = new Token(TokenType.SEMICOLON, ";", new SourceLocation(1, 6, 5));
        Token eof = Token.eof(new SourceLocation(1, 7, 6));

        TokenStream stream = new ListTokenStream(List.of(t1, t2, t3, eof));

        assertEquals(0, stream.position());
        assertFalse(stream.isAtEnd());
        assertThrows(IllegalStateException.class, stream::previous);

        assertEquals(t1, stream.advance());
        assertEquals(1, stream.position());
        assertEquals(t1, stream.previous());
        assertEquals(t1.location(), stream.previous().location());

        assertEquals(t2, stream.advance());
        assertEquals(2, stream.position());
        assertEquals(t2, stream.previous());

        assertEquals(t3, stream.advance());
        assertEquals(3, stream.position());
        assertEquals(t3, stream.previous());
        assertTrue(stream.isAtEnd());
    }

    @Test
    void performsSingleAndMultipleTokenLookaheadWithoutMovingCursor() {
        Token t1 = new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0));
        Token t2 = new Token(TokenType.IDENTIFIER, "val", new SourceLocation(1, 5, 4));
        Token t3 = new Token(TokenType.ASSIGN, "=", new SourceLocation(1, 9, 8));
        Token eof = Token.eof(new SourceLocation(1, 10, 9));

        TokenStream stream = new ListTokenStream(List.of(t1, t2, t3, eof));

        assertEquals(t1, stream.peek());
        assertEquals(t1, stream.peek(0));
        assertEquals(t2, stream.peek(1));
        assertEquals(t3, stream.peek(2));
        assertEquals(eof, stream.peek(3));
        assertEquals(eof, stream.peek(100));
        assertEquals(eof, stream.peek(Integer.MAX_VALUE));
        assertEquals(0, stream.position());
    }

    @Test
    void checksAndMatchesSingleAndMultipleTokenTypes() {
        Token t1 = new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0));
        Token t2 = new Token(TokenType.IDENTIFIER, "x", new SourceLocation(1, 5, 4));
        Token eof = Token.eof(new SourceLocation(1, 6, 5));

        TokenStream stream = new ListTokenStream(List.of(t1, t2, eof));

        assertTrue(stream.check(TokenType.INT));
        assertFalse(stream.check(TokenType.LONG));
        assertEquals(0, stream.position());

        assertFalse(stream.match(TokenType.LONG));
        assertFalse(stream.match(TokenType.BYTE, TokenType.SHORT, TokenType.LONG));
        assertEquals(0, stream.position());

        assertTrue(stream.match(TokenType.BYTE, TokenType.INT));
        assertEquals(1, stream.position());
        assertEquals(t1, stream.previous());

        assertTrue(stream.match(TokenType.IDENTIFIER));
        assertEquals(2, stream.position());
        assertTrue(stream.isAtEnd());

        assertTrue(stream.check(TokenType.EOF));
        assertFalse(stream.match(TokenType.EOF));
        assertFalse(stream.match(TokenType.EOF, TokenType.INT));
        assertEquals(2, stream.position());
    }

    @Test
    void consumesExpectedTokensAndThrowsDetailedErrorOnMismatch() {
        SourceLocation identLoc = new SourceLocation(2, 5, 18);
        Token t1 = new Token(TokenType.INT, "int", new SourceLocation(2, 1, 14));
        Token t2 = new Token(TokenType.IDENTIFIER, "count", identLoc);
        Token eof = Token.eof(new SourceLocation(2, 10, 23));

        TokenStream stream = new ListTokenStream(List.of(t1, t2, eof));

        assertEquals(t1, stream.consume(TokenType.INT, "Expected type keyword"));
        assertEquals(1, stream.position());

        UnexpectedTokenException error = assertThrows(
                UnexpectedTokenException.class,
                () -> stream.consume(TokenType.SEMICOLON, "Expected ';' after declaration"));

        assertEquals(TokenType.SEMICOLON, error.getExpectedType());
        assertEquals(t2, error.getActualToken());
        assertEquals(identLoc, error.getLocation());
        assertTrue(error.getMessage().contains("Expected ';' after declaration"));
        assertTrue(error.getMessage().contains("SEMICOLON"));
        assertTrue(error.getMessage().contains("IDENTIFIER"));
        assertTrue(error.getMessage().contains("2:5"));
        assertEquals(1, stream.position());

        assertEquals(t2, stream.consume(TokenType.IDENTIFIER, "Expected identifier"));
        assertEquals(eof, stream.consume(TokenType.EOF, "Expected end of file"));
        assertTrue(stream.isAtEnd());
        assertEquals(2, stream.position());
    }

    @Test
    void handlesEofOnlyInputAndRepeatedEofReadsSafely() {
        Token eof = Token.eof(SourceLocation.START);
        TokenStream stream = new ListTokenStream(List.of(eof));

        assertEquals(0, stream.position());
        assertTrue(stream.isAtEnd());
        assertTrue(stream.check(TokenType.EOF));
        assertThrows(IllegalStateException.class, stream::previous);

        for (int i = 0; i < 5; i++) {
            assertEquals(eof, stream.peek());
            assertEquals(eof, stream.peek(i));
            assertEquals(eof, stream.advance());
            assertTrue(stream.isAtEnd());
            assertEquals(0, stream.position());
        }
    }

    @Test
    void rejectsInvalidCollectionsNullElementsAndInvalidArguments() {
        Token intToken = new Token(TokenType.INT, "int", SourceLocation.START);
        Token eof = Token.eof(new SourceLocation(1, 4, 3));

        assertThrows(IllegalArgumentException.class, () -> new ListTokenStream(null));
        assertThrows(IllegalArgumentException.class, () -> new ListTokenStream(List.of()));

        List<Token> containsNull = new ArrayList<>();
        containsNull.add(intToken);
        containsNull.add(null);
        containsNull.add(eof);
        assertThrows(IllegalArgumentException.class, () -> new ListTokenStream(containsNull));

        assertThrows(IllegalArgumentException.class,
                () -> new ListTokenStream(List.of(intToken)));
        assertThrows(IllegalArgumentException.class,
                () -> new ListTokenStream(List.of(eof, intToken, eof)));
        assertThrows(IllegalArgumentException.class,
                () -> new ListTokenStream(List.of(intToken, eof, eof)));

        TokenStream valid = new ListTokenStream(List.of(intToken, eof));
        assertThrows(IllegalArgumentException.class, () -> valid.peek(-1));
        assertThrows(IllegalArgumentException.class, () -> valid.peek(Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> valid.check(null));
        assertThrows(IllegalArgumentException.class, () -> valid.match((TokenType) null));
        assertThrows(IllegalArgumentException.class, () -> valid.match((TokenType[]) null));
        assertThrows(IllegalArgumentException.class,
                () -> valid.match(TokenType.INT, null));
        assertThrows(IllegalArgumentException.class,
                () -> valid.consume(null, "Expected token"));
        assertThrows(IllegalArgumentException.class,
                () -> valid.consume(TokenType.INT, null));
        assertThrows(IllegalArgumentException.class,
                () -> valid.consume(TokenType.INT, "   "));
    }

    @Test
    void defensivelyCopiesInputTokenList() {
        Token intToken = new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0));
        Token identToken = new Token(TokenType.IDENTIFIER, "a", new SourceLocation(1, 5, 4));
        Token eof = Token.eof(new SourceLocation(1, 6, 5));

        List<Token> mutableInput = new ArrayList<>(List.of(intToken, identToken, eof));
        TokenStream stream = new ListTokenStream(mutableInput);

        mutableInput.clear();
        mutableInput.add(Token.eof(SourceLocation.START));

        assertFalse(stream.isAtEnd());
        assertEquals(intToken, stream.advance());
        assertEquals(identToken, stream.advance());
        assertTrue(stream.isAtEnd());
    }

    @Test
    void remainsCompatibleWithAbstractTypeParser() {
        TokenStream stream = new ListTokenStream(List.of(
                new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0)),
                new Token(TokenType.IDENTIFIER, "value", new SourceLocation(1, 5, 4)),
                Token.eof(new SourceLocation(1, 10, 9))
        ));

        TypeDescriptor intDescriptor = new TypeDescriptor() {
            @Override
            public String typeName() {
                return "int";
            }

            @Override
            public TypeCategory category() {
                return TypeCategory.INTEGRAL;
            }

            @Override
            public int byteSize() {
                return 4;
            }

            @Override
            public boolean isSigned() {
                return true;
            }

            @Override
            public boolean isPrimitive() {
                return true;
            }

            @Override
            public boolean isAssignableTo(TypeDescriptor target) {
                return target != null && "int".equals(target.typeName());
            }
        };

        AbstractTypeParser<AstNode> intParser = new AbstractTypeParser<>() {
            @Override
            public TypeDescriptor getTypeDescriptor() {
                return intDescriptor;
            }

            @Override
            protected boolean supportsCurrentTokens(TokenStream tokens) {
                return tokens.check(TokenType.INT);
            }

            @Override
            protected AstNode parseTypeConstruct(TokenStream tokens) {
                Token keyword = tokens.consume(TokenType.INT, "Expected 'int'");
                return new AstNode() {
                    @Override
                    public String nodeKind() {
                        return "TypeKeyword:" + keyword.lexeme();
                    }

                    @Override
                    public Optional<TypeDescriptor> typeDescriptor() {
                        return Optional.of(intDescriptor);
                    }
                };
            }
        };

        assertTrue(intParser.supports(stream));
        AstNode parsed = intParser.parse(stream);
        assertEquals("TypeKeyword:int", parsed.nodeKind());
        assertEquals(1, stream.position());
        assertEquals(TokenType.IDENTIFIER, stream.peek().type());
    }

    @Test
    void integrationExampleProcessesIntAdditionDeclarationTokenSequence() {
        // Source representation: "int a = 10 + 20;"
        Token tInt = new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0));
        Token tIdent = new Token(TokenType.IDENTIFIER, "a", new SourceLocation(1, 5, 4));
        Token tAssign = new Token(TokenType.ASSIGN, "=", new SourceLocation(1, 7, 6));
        Token tTen = new Token(TokenType.INTEGER_LITERAL, "10", new SourceLocation(1, 9, 8));
        Token tPlus = new Token(TokenType.PLUS, "+", new SourceLocation(1, 12, 11));
        Token tTwenty = new Token(TokenType.INTEGER_LITERAL, "20", new SourceLocation(1, 14, 13));
        Token tSemi = new Token(TokenType.SEMICOLON, ";", new SourceLocation(1, 16, 15));
        Token tEof = Token.eof(new SourceLocation(1, 17, 16));

        TokenStream stream = new ListTokenStream(
                List.of(tInt, tIdent, tAssign, tTen, tPlus, tTwenty, tSemi, tEof));

        // Lookahead before consuming anything
        assertEquals(0, stream.position());
        assertSame(tInt, stream.peek());
        assertSame(tIdent, stream.peek(1));
        assertSame(tAssign, stream.peek(2));
        assertEquals(0, stream.position());

        // Match declaration type keyword
        assertTrue(stream.match(TokenType.BYTE, TokenType.SHORT, TokenType.INT, TokenType.LONG));
        assertEquals(1, stream.position());
        assertSame(tInt, stream.previous());

        // Consume identifier and '='
        Token variable = stream.consume(TokenType.IDENTIFIER, "Expected variable identifier");
        assertEquals("a", variable.lexeme());
        assertEquals(new SourceLocation(1, 5, 4), variable.location());
        assertTrue(stream.match(TokenType.ASSIGN));
        assertEquals(3, stream.position());

        // Parse binary expression tokens sequentially
        Token lhs = stream.consume(TokenType.INTEGER_LITERAL, "Expected left integer operand");
        assertEquals("10", lhs.lexeme());
        assertTrue(stream.check(TokenType.PLUS));
        Token op = stream.advance();
        assertEquals(TokenType.PLUS, op.type());
        Token rhs = stream.consume(TokenType.INTEGER_LITERAL, "Expected right integer operand");
        assertEquals("20", rhs.lexeme());
        assertEquals(6, stream.position());

        // Consume terminating semicolon and verify EOF
        Token semi = stream.consume(TokenType.SEMICOLON, "Expected ';' after declaration");
        assertEquals(";", semi.lexeme());
        assertEquals(7, stream.position());
        assertTrue(stream.isAtEnd());
        assertSame(tEof, stream.peek());
        assertSame(tEof, stream.advance());
    }
}
