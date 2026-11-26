package rkcc.compiler.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import rkcc.compiler.ast.AstNode;
import rkcc.compiler.core.AbstractTypeParser;
import rkcc.compiler.diagnostics.SourceLocation;
import rkcc.compiler.types.TypeCategory;
import rkcc.compiler.types.TypeDescriptor;

/**
 * Unit tests for {@link Token} creation, validation, immutability, equality,
 * and compatibility with {@link TokenStream} and {@link AbstractTypeParser}.
 */
class TokenTest {

    @Test
    void createsPrimitiveIdentifierLiteralAndOperatorTokensPreservingOriginalLexemes() {
        SourceLocation loc1 = new SourceLocation(1, 1, 0);
        SourceLocation loc2 = new SourceLocation(1, 5, 4);
        SourceLocation loc3 = new SourceLocation(2, 3, 20);

        Token intToken = new Token(TokenType.INT, "int", loc1);
        Token stringTypeIdent = new Token(TokenType.IDENTIFIER, "String", loc2);
        Token oversizedLiteral = new Token(TokenType.INTEGER_LITERAL, "999999999999999999999999", loc3);
        Token minusOperator = new Token(TokenType.MINUS, "-", loc1);
        Token bitwiseShift = new Token(TokenType.UNSIGNED_RIGHT_SHIFT, ">>>", loc2);

        assertTrue(intToken.is(TokenType.INT));
        assertTrue(intToken.isPrimitiveTypeKeyword());
        assertFalse(stringTypeIdent.isPrimitiveTypeKeyword());
        assertEquals("String", stringTypeIdent.lexeme());
        assertEquals("999999999999999999999999", oversizedLiteral.lexeme());
        assertEquals(TokenType.MINUS, minusOperator.type());
        assertEquals(">>>", bitwiseShift.lexeme());
        assertEquals(loc3, oversizedLiteral.location());
    }

    @Test
    void supportsEofTokensWithEmptyLexeme() {
        SourceLocation eofLocation = new SourceLocation(10, 1, 180);
        Token eof = Token.eof(eofLocation);
        Token directEof = new Token(TokenType.EOF, "", eofLocation);

        assertEquals(TokenType.EOF, eof.type());
        assertEquals("", eof.lexeme());
        assertEquals(eofLocation, eof.location());
        assertEquals(eof, directEof);
    }

    @Test
    void rejectsInvalidConstructorArgumentsAndEmptyNonEofLexemes() {
        assertThrows(IllegalArgumentException.class,
                () -> new Token(null, "int", SourceLocation.START));
        assertThrows(IllegalArgumentException.class,
                () -> new Token(TokenType.INT, null, SourceLocation.START));
        assertThrows(IllegalArgumentException.class,
                () -> new Token(TokenType.INT, "int", null));
        assertThrows(IllegalArgumentException.class,
                () -> new Token(TokenType.IDENTIFIER, "", SourceLocation.START));
        assertThrows(IllegalArgumentException.class,
                () -> new Token(TokenType.EOF, "EOF", SourceLocation.START));
    }

    @Test
    void verifiesEqualityHashCodeToStringAndImmutability() {
        SourceLocation loc = new SourceLocation(2, 10, 25);
        Token first = new Token(TokenType.PLUS_ASSIGN, "+=", loc);
        Token copy = new Token(TokenType.PLUS_ASSIGN, "+=", new SourceLocation(2, 10, 25));
        Token differentLexeme = new Token(TokenType.INTEGER_LITERAL, "0x10", loc);
        Token decimalSixteen = new Token(TokenType.INTEGER_LITERAL, "16", loc);

        assertEquals(first, copy);
        assertEquals(first.hashCode(), copy.hashCode());
        assertNotEquals(differentLexeme, decimalSixteen);
        assertEquals("Token[type=PLUS_ASSIGN, lexeme='+=', location=2:10]", first.toString());
        assertTrue(Token.class.isRecord());
    }

    @Test
    void integratesWithTokenStreamAndAbstractTypeParser() {
        List<Token> tokens = List.of(
                new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0)),
                new Token(TokenType.IDENTIFIER, "x", new SourceLocation(1, 5, 4))
        );
        TokenStream stream = new ListTokenStream(tokens);

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

        AbstractTypeParser<AstNode> parser = new AbstractTypeParser<>() {
            @Override
            public TypeDescriptor getTypeDescriptor() {
                return intDescriptor;
            }

            @Override
            protected boolean supportsCurrentTokens(TokenStream ts) {
                return ts.peek().is(TokenType.INT);
            }

            @Override
            protected AstNode parseTypeConstruct(TokenStream ts) {
                Token typeToken = ts.advance();
                return new AstNode() {
                    @Override
                    public String nodeKind() {
                        return "TestTypeKeyword:" + typeToken.lexeme();
                    }

                    @Override
                    public Optional<TypeDescriptor> typeDescriptor() {
                        return Optional.of(intDescriptor);
                    }
                };
            }
        };

        assertTrue(parser.supports(stream));
        AstNode node = parser.parse(stream);
        assertEquals("TestTypeKeyword:int", node.nodeKind());
        assertTrue(stream.match(TokenType.IDENTIFIER));
        assertTrue(stream.isAtEnd());
    }

    private static final class ListTokenStream implements TokenStream {
        private static final Token EOF = Token.eof(SourceLocation.START);
        private final List<Token> tokens;
        private int cursor = 0;

        private ListTokenStream(List<Token> tokens) {
            this.tokens = List.copyOf(tokens);
        }

        @Override
        public Token peek() {
            return peek(0);
        }

        @Override
        public Token peek(int offset) {
            int idx = cursor + offset;
            return (idx >= 0 && idx < tokens.size()) ? tokens.get(idx) : EOF;
        }

        @Override
        public Token advance() {
            return isAtEnd() ? EOF : tokens.get(cursor++);
        }

        @Override
        public boolean isAtEnd() {
            return cursor >= tokens.size();
        }

        @Override
        public boolean match(TokenType expectedType) {
            if (!isAtEnd() && peek().is(expectedType)) {
                advance();
                return true;
            }
            return false;
        }
    }
}
