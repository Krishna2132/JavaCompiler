package rkcc.compiler.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying {@link TokenType} coverage, categorization, and keyword lookup.
 */
class TokenTypeTest {

    @Test
    void coversAllPrimitiveTypeKeywordsAndTreatsStringAsIdentifier() {
        List<TokenType> primitives = List.of(
                TokenType.BYTE,
                TokenType.SHORT,
                TokenType.INT,
                TokenType.LONG,
                TokenType.FLOAT,
                TokenType.DOUBLE,
                TokenType.CHAR,
                TokenType.BOOLEAN,
                TokenType.VOID
        );

        for (TokenType primitive : primitives) {
            assertTrue(primitive.isPrimitiveTypeKeyword());
            assertTrue(primitive.fixedLexeme().isPresent());
            assertEquals(Optional.of(primitive), TokenType.fromKeyword(primitive.fixedLexeme().get()));
        }

        assertEquals(Optional.empty(), TokenType.fromKeyword("String"));
        assertEquals(Optional.empty(), TokenType.fromKeyword("myVar"));
        assertEquals(Optional.empty(), TokenType.fromKeyword(null));
        assertFalse(TokenType.IDENTIFIER.isPrimitiveTypeKeyword());
    }

    @Test
    void coversIdentifiersLiteralsAndKeywordLiterals() {
        assertTrue(TokenType.INTEGER_LITERAL.isLiteral());
        assertTrue(TokenType.FLOATING_POINT_LITERAL.isLiteral());
        assertTrue(TokenType.CHARACTER_LITERAL.isLiteral());
        assertTrue(TokenType.STRING_LITERAL.isLiteral());
        assertTrue(TokenType.TRUE.isLiteral());
        assertTrue(TokenType.FALSE.isLiteral());
        assertTrue(TokenType.NULL.isLiteral());
        assertFalse(TokenType.IDENTIFIER.isLiteral());

        assertEquals(Optional.of(TokenType.TRUE), TokenType.fromKeyword("true"));
        assertEquals(Optional.of(TokenType.FALSE), TokenType.fromKeyword("false"));
        assertEquals(Optional.of(TokenType.NULL), TokenType.fromKeyword("null"));
        assertTrue(TokenType.IDENTIFIER.fixedLexeme().isEmpty());
        assertTrue(TokenType.INTEGER_LITERAL.fixedLexeme().isEmpty());
    }

    @Test
    void coversArithmeticAssignmentBitwiseComparisonLogicalAndPunctuationTokens() {
        assertTrue(TokenType.PLUS.isArithmeticOperator());
        assertTrue(TokenType.MINUS.isArithmeticOperator());
        assertTrue(TokenType.STAR.isArithmeticOperator());
        assertTrue(TokenType.SLASH.isArithmeticOperator());
        assertTrue(TokenType.PERCENT.isArithmeticOperator());

        List<TokenType> assignments = List.of(
                TokenType.ASSIGN,
                TokenType.PLUS_ASSIGN,
                TokenType.MINUS_ASSIGN,
                TokenType.STAR_ASSIGN,
                TokenType.SLASH_ASSIGN,
                TokenType.PERCENT_ASSIGN,
                TokenType.BITWISE_AND_ASSIGN,
                TokenType.BITWISE_OR_ASSIGN,
                TokenType.BITWISE_XOR_ASSIGN,
                TokenType.LEFT_SHIFT_ASSIGN,
                TokenType.RIGHT_SHIFT_ASSIGN,
                TokenType.UNSIGNED_RIGHT_SHIFT_ASSIGN
        );
        assignments.forEach(tokenType -> assertTrue(tokenType.isAssignmentOperator()));

        List<TokenType> bitwise = List.of(
                TokenType.BITWISE_AND,
                TokenType.BITWISE_OR,
                TokenType.BITWISE_XOR,
                TokenType.BITWISE_COMPLEMENT,
                TokenType.LEFT_SHIFT,
                TokenType.RIGHT_SHIFT,
                TokenType.UNSIGNED_RIGHT_SHIFT
        );
        bitwise.forEach(tokenType -> assertTrue(tokenType.isBitwiseOperator()));

        List<TokenType> comparisons = List.of(
                TokenType.EQUAL_EQUAL,
                TokenType.NOT_EQUAL,
                TokenType.LESS_THAN,
                TokenType.GREATER_THAN,
                TokenType.LESS_THAN_OR_EQUAL,
                TokenType.GREATER_THAN_OR_EQUAL
        );
        comparisons.forEach(tokenType -> assertTrue(tokenType.isComparisonOperator()));

        assertTrue(TokenType.LOGICAL_AND.isLogicalOperator());
        assertTrue(TokenType.LOGICAL_OR.isLogicalOperator());
        assertTrue(TokenType.LOGICAL_NOT.isLogicalOperator());

        assertEquals("++", TokenType.INCREMENT.fixedLexeme().orElseThrow());
        assertEquals("--", TokenType.DECREMENT.fixedLexeme().orElseThrow());
        assertEquals("(", TokenType.LEFT_PAREN.fixedLexeme().orElseThrow());
        assertEquals(")", TokenType.RIGHT_PAREN.fixedLexeme().orElseThrow());
        assertEquals("{", TokenType.LEFT_BRACE.fixedLexeme().orElseThrow());
        assertEquals("}", TokenType.RIGHT_BRACE.fixedLexeme().orElseThrow());
        assertEquals("[", TokenType.LEFT_BRACKET.fixedLexeme().orElseThrow());
        assertEquals("]", TokenType.RIGHT_BRACKET.fixedLexeme().orElseThrow());
        assertEquals(";", TokenType.SEMICOLON.fixedLexeme().orElseThrow());
        assertEquals(",", TokenType.COMMA.fixedLexeme().orElseThrow());
        assertEquals(".", TokenType.DOT.fixedLexeme().orElseThrow());
        assertEquals(":", TokenType.COLON.fixedLexeme().orElseThrow());
        assertEquals("?", TokenType.QUESTION_MARK.fixedLexeme().orElseThrow());
        assertEquals("", TokenType.EOF.fixedLexeme().orElseThrow());
    }
}
