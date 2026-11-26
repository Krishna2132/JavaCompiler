package rkcc.compiler.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import rkcc.compiler.diagnostics.SourceLocation;

/**
 * Comprehensive unit and example tests for {@link Lexer} and {@link LexicalException}.
 */
class LexerTest {

    private final Lexer lexer = new Lexer();

    @Test
    void tokenizesPrimitiveKeywordsBooleanNullAndIdentifiersWithDistinction() {
        String source = "byte short int long float double char boolean void true false null "
                + "String integerValue intCount booleanFlag _temp $val";

        List<Token> tokens = lexer.tokenize(source);

        assertEquals(List.of(
                new Token(TokenType.BYTE, "byte", new SourceLocation(1, 1, 0)),
                new Token(TokenType.SHORT, "short", new SourceLocation(1, 6, 5)),
                new Token(TokenType.INT, "int", new SourceLocation(1, 12, 11)),
                new Token(TokenType.LONG, "long", new SourceLocation(1, 16, 15)),
                new Token(TokenType.FLOAT, "float", new SourceLocation(1, 21, 20)),
                new Token(TokenType.DOUBLE, "double", new SourceLocation(1, 27, 26)),
                new Token(TokenType.CHAR, "char", new SourceLocation(1, 34, 33)),
                new Token(TokenType.BOOLEAN, "boolean", new SourceLocation(1, 39, 38)),
                new Token(TokenType.VOID, "void", new SourceLocation(1, 47, 46)),
                new Token(TokenType.TRUE, "true", new SourceLocation(1, 52, 51)),
                new Token(TokenType.FALSE, "false", new SourceLocation(1, 57, 56)),
                new Token(TokenType.NULL, "null", new SourceLocation(1, 63, 62)),
                new Token(TokenType.IDENTIFIER, "String", new SourceLocation(1, 68, 67)),
                new Token(TokenType.IDENTIFIER, "integerValue", new SourceLocation(1, 75, 74)),
                new Token(TokenType.IDENTIFIER, "intCount", new SourceLocation(1, 88, 87)),
                new Token(TokenType.IDENTIFIER, "booleanFlag", new SourceLocation(1, 97, 96)),
                new Token(TokenType.IDENTIFIER, "_temp", new SourceLocation(1, 109, 108)),
                new Token(TokenType.IDENTIFIER, "$val", new SourceLocation(1, 115, 114)),
                Token.eof(new SourceLocation(1, 119, 118))
        ), tokens);
    }

    @Test
    void tokenizesIntegerAndFloatingPointLiteralsWithSuffixesAndSeparators() {
        String source = "0 42 1_000_000 100000L 0l 0b1010_0101 0B11L 077 0_7_7L 0xFF 0X1a_2BL "
                + "3.14 .5 5. 1_2.3_4 1e5 1E-5 1.5e+10 3.14f 2.0D 10f 08.5 -42 9999999999999999999999";

        List<Token> tokens = lexer.tokenize(source);

        List<TokenType> expectedTypes = List.of(
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.FLOATING_POINT_LITERAL,
                TokenType.MINUS,
                TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL,
                TokenType.EOF
        );
        assertEquals(expectedTypes, tokens.stream().map(Token::type).toList());
        assertEquals("1_000_000", tokens.get(2).lexeme());
        assertEquals("0xFF", tokens.get(9).lexeme());
        assertEquals("1.5e+10", tokens.get(17).lexeme());
        assertEquals("-", tokens.get(22).lexeme());
        assertEquals("42", tokens.get(23).lexeme());
        assertEquals("9999999999999999999999", tokens.get(24).lexeme());
    }

    @Test
    void tokenizesStringAndCharacterLiteralsWithEscapeSequences() {
        String source = "\"hello\" \"\\\"quoted\\\"\\n\\t\\\\ // not comment /* nope */\" 'A' '\\n' '\\'' '\\\\' '\\0' '\\u0041'";

        List<Token> tokens = lexer.tokenize(source);

        assertEquals(List.of(
                new Token(TokenType.STRING_LITERAL, "\"hello\"", new SourceLocation(1, 1, 0)),
                new Token(TokenType.STRING_LITERAL,
                        "\"\\\"quoted\\\"\\n\\t\\\\ // not comment /* nope */\"",
                        new SourceLocation(1, 9, 8)),
                new Token(TokenType.CHARACTER_LITERAL, "'A'", new SourceLocation(1, 54, 53)),
                new Token(TokenType.CHARACTER_LITERAL, "'\\n'", new SourceLocation(1, 58, 57)),
                new Token(TokenType.CHARACTER_LITERAL, "'\\''", new SourceLocation(1, 63, 62)),
                new Token(TokenType.CHARACTER_LITERAL, "'\\\\'", new SourceLocation(1, 68, 67)),
                new Token(TokenType.CHARACTER_LITERAL, "'\\0'", new SourceLocation(1, 73, 72)),
                new Token(TokenType.CHARACTER_LITERAL, "'\\u0041'", new SourceLocation(1, 78, 77)),
                Token.eof(new SourceLocation(1, 86, 85))
        ), tokens);
    }

    @Test
    void tracksUnicodeIdentifiersAndSurrogateCodePointsAccurately() {
        String source = "int αβ = 1;\nString café = \"\uD83D\uDE00\";";
        List<Token> tokens = lexer.tokenize(source);

        assertEquals(new Token(TokenType.IDENTIFIER, "αβ", new SourceLocation(1, 5, 4)), tokens.get(1));
        assertEquals(new Token(TokenType.IDENTIFIER, "café", new SourceLocation(2, 8, 19)), tokens.get(6));
        assertEquals(new Token(TokenType.STRING_LITERAL, "\"\uD83D\uDE00\"", new SourceLocation(2, 15, 26)),
                tokens.get(8));
    }

    @Test
    void tokenizesAllOperatorsWithLongestMatchAndDelimiters() {
        String source = "+ - * / % = += -= *= /= %= & | ^ ~ << >> >>> &= |= ^= <<= >>= >>>= "
                + "== != < > <= >= && || ! ++ -- ( ) { } [ ] ; , . : ?";

        List<Token> tokens = lexer.tokenize(source);

        List<TokenType> expected = List.of(
                TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH, TokenType.PERCENT,
                TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN, TokenType.STAR_ASSIGN,
                TokenType.SLASH_ASSIGN, TokenType.PERCENT_ASSIGN,
                TokenType.BITWISE_AND, TokenType.BITWISE_OR, TokenType.BITWISE_XOR, TokenType.BITWISE_COMPLEMENT,
                TokenType.LEFT_SHIFT, TokenType.RIGHT_SHIFT, TokenType.UNSIGNED_RIGHT_SHIFT,
                TokenType.BITWISE_AND_ASSIGN, TokenType.BITWISE_OR_ASSIGN, TokenType.BITWISE_XOR_ASSIGN,
                TokenType.LEFT_SHIFT_ASSIGN, TokenType.RIGHT_SHIFT_ASSIGN, TokenType.UNSIGNED_RIGHT_SHIFT_ASSIGN,
                TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL, TokenType.LESS_THAN, TokenType.GREATER_THAN,
                TokenType.LESS_THAN_OR_EQUAL, TokenType.GREATER_THAN_OR_EQUAL,
                TokenType.LOGICAL_AND, TokenType.LOGICAL_OR, TokenType.LOGICAL_NOT,
                TokenType.INCREMENT, TokenType.DECREMENT,
                TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN,
                TokenType.LEFT_BRACE, TokenType.RIGHT_BRACE,
                TokenType.LEFT_BRACKET, TokenType.RIGHT_BRACKET,
                TokenType.SEMICOLON, TokenType.COMMA, TokenType.DOT, TokenType.COLON, TokenType.QUESTION_MARK,
                Token.eof(SourceLocation.START).type()
        );
        assertEquals(expected, tokens.stream().map(Token::type).toList());

        List<Token> overlapping = lexer.tokenize(">>>=>>>=++--&&=");
        assertEquals(List.of(
                new Token(TokenType.UNSIGNED_RIGHT_SHIFT_ASSIGN, ">>>=", new SourceLocation(1, 1, 0)),
                new Token(TokenType.UNSIGNED_RIGHT_SHIFT_ASSIGN, ">>>=", new SourceLocation(1, 5, 4)),
                new Token(TokenType.INCREMENT, "++", new SourceLocation(1, 9, 8)),
                new Token(TokenType.DECREMENT, "--", new SourceLocation(1, 11, 10)),
                new Token(TokenType.LOGICAL_AND, "&&", new SourceLocation(1, 13, 12)),
                new Token(TokenType.ASSIGN, "=", new SourceLocation(1, 15, 14)),
                Token.eof(new SourceLocation(1, 16, 15))
        ), overlapping);
    }

    @Test
    void tracksLineColumnOffsetAcrossWhitespaceCommentsTabsUnixAndWindowsLineEndings() {
        String unixSource = "\tint a = 1; // line comment\n"
                + "/* block\n"
                + "   comment */\n"
                + "/** doc comment */\n"
                + "int b = 2;";

        List<Token> unixTokens = lexer.tokenize(unixSource);
        assertEquals(new Token(TokenType.INT, "int", new SourceLocation(1, 2, 1)), unixTokens.get(0));
        assertEquals(new Token(TokenType.IDENTIFIER, "a", new SourceLocation(1, 6, 5)), unixTokens.get(1));
        assertEquals(new Token(TokenType.INT, "int", new SourceLocation(5, 1, 70)), unixTokens.get(5));
        assertEquals(new Token(TokenType.SEMICOLON, ";", new SourceLocation(5, 10, 79)), unixTokens.get(9));
        assertEquals(Token.eof(new SourceLocation(5, 11, 80)), unixTokens.get(10));

        String crlfSource = "int x = 1;\r\nint y = 2;\r\n";
        List<Token> crlfTokens = lexer.tokenize(crlfSource);
        assertEquals(new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0)), crlfTokens.get(0));
        assertEquals(new Token(TokenType.INT, "int", new SourceLocation(2, 1, 12)), crlfTokens.get(5));
        assertEquals(Token.eof(new SourceLocation(3, 1, 24)), crlfTokens.get(10));
    }

    @Test
    void handlesEmptyAndWhitespaceOnlyInputWithEof() {
        assertEquals(List.of(Token.eof(SourceLocation.START)), lexer.tokenize(""));
        assertEquals(List.of(Token.eof(new SourceLocation(2, 3, 5))), lexer.tokenize("  \n\t "));

        TokenStream emptyStream = lexer.tokenizeToStream("");
        assertTrue(emptyStream.isAtEnd());
        assertEquals(Token.eof(SourceLocation.START), emptyStream.peek());
    }

    @Test
    void rejectsNullSourceAndReportsMeaningfulLexicalErrorsWithSourceLocations() {
        assertThrows(IllegalArgumentException.class, () -> lexer.tokenize(null));
        assertThrows(IllegalArgumentException.class, () -> lexer.tokenizeToStream(null));

        LexicalException unexpectedChar = assertThrows(
                LexicalException.class, () -> lexer.tokenize("int a = @;"));
        assertEquals(new SourceLocation(1, 9, 8), unexpectedChar.getLocation());
        assertEquals("@", unexpectedChar.getOffendingLexeme());

        LexicalException badBinary = assertThrows(
                LexicalException.class, () -> lexer.tokenize("0b102"));
        assertEquals(new SourceLocation(1, 1, 0), badBinary.getLocation());

        LexicalException emptyBinary = assertThrows(
                LexicalException.class, () -> lexer.tokenize("0b"));
        assertEquals(new SourceLocation(1, 1, 0), emptyBinary.getLocation());

        LexicalException badOctal = assertThrows(
                LexicalException.class, () -> lexer.tokenize("089"));
        assertEquals(new SourceLocation(1, 1, 0), badOctal.getLocation());

        LexicalException badHex = assertThrows(
                LexicalException.class, () -> lexer.tokenize("0xG"));
        assertEquals(new SourceLocation(1, 1, 0), badHex.getLocation());

        LexicalException badHexFloat = assertThrows(
                LexicalException.class, () -> lexer.tokenize("0x1.0p2"));
        assertEquals(new SourceLocation(1, 1, 0), badHexFloat.getLocation());

        LexicalException trailingUnderscore = assertThrows(
                LexicalException.class, () -> lexer.tokenize("123_"));
        assertEquals(new SourceLocation(1, 1, 0), trailingUnderscore.getLocation());

        LexicalException badExponent = assertThrows(
                LexicalException.class, () -> lexer.tokenize("1.5e+"));
        assertEquals(new SourceLocation(1, 1, 0), badExponent.getLocation());

        LexicalException multiDot = assertThrows(
                LexicalException.class, () -> lexer.tokenize("1.2.3"));
        assertEquals(new SourceLocation(1, 1, 0), multiDot.getLocation());

        LexicalException trailingLetters = assertThrows(
                LexicalException.class, () -> lexer.tokenize("123abc"));
        assertEquals(new SourceLocation(1, 1, 0), trailingLetters.getLocation());

        LexicalException unterminatedString = assertThrows(
                LexicalException.class, () -> lexer.tokenize("String s = \"abc\n"));
        assertEquals(new SourceLocation(1, 12, 11), unterminatedString.getLocation());

        LexicalException invalidEscape = assertThrows(
                LexicalException.class, () -> lexer.tokenize("\"bad\\q\""));
        assertEquals(new SourceLocation(1, 5, 4), invalidEscape.getLocation());

        LexicalException emptyChar = assertThrows(
                LexicalException.class, () -> lexer.tokenize("''"));
        assertEquals(new SourceLocation(1, 1, 0), emptyChar.getLocation());

        LexicalException multiChar = assertThrows(
                LexicalException.class, () -> lexer.tokenize("'ab'"));
        assertEquals(new SourceLocation(1, 1, 0), multiChar.getLocation());

        LexicalException unterminatedChar = assertThrows(
                LexicalException.class, () -> lexer.tokenize("'a"));
        assertEquals(new SourceLocation(1, 1, 0), unterminatedChar.getLocation());

        LexicalException unterminatedComment = assertThrows(
                LexicalException.class, () -> lexer.tokenize("int x = 1; /* unclosed"));
        assertEquals(new SourceLocation(1, 12, 11), unterminatedComment.getLocation());
    }

    @Test
    void verifiesIntegrationExamplesWithTokenStream() {
        // Example 1
        TokenStream ex1 = lexer.tokenizeToStream("int a = 10;\nint b = 20;\nint c = a + b;");
        assertFalse(ex1.isAtEnd());
        assertEquals(new Token(TokenType.INT, "int", new SourceLocation(1, 1, 0)), ex1.consume(TokenType.INT, "int"));
        assertEquals(new Token(TokenType.IDENTIFIER, "a", new SourceLocation(1, 5, 4)), ex1.consume(TokenType.IDENTIFIER, "id"));
        assertTrue(ex1.match(TokenType.ASSIGN));
        assertEquals("10", ex1.consume(TokenType.INTEGER_LITERAL, "10").lexeme());
        assertTrue(ex1.match(TokenType.SEMICOLON));
        assertEquals(new SourceLocation(2, 1, 12), ex1.peek().location());

        // Example 2
        List<Token> ex2 = lexer.tokenize("byte x = 127;\nshort y = 32000;\nlong z = 100000L;");
        assertEquals(List.of(
                TokenType.BYTE, TokenType.IDENTIFIER, TokenType.ASSIGN, TokenType.INTEGER_LITERAL, TokenType.SEMICOLON,
                TokenType.SHORT, TokenType.IDENTIFIER, TokenType.ASSIGN, TokenType.INTEGER_LITERAL, TokenType.SEMICOLON,
                TokenType.LONG, TokenType.IDENTIFIER, TokenType.ASSIGN, TokenType.INTEGER_LITERAL, TokenType.SEMICOLON,
                TokenType.EOF
        ), ex2.stream().map(Token::type).toList());
        assertEquals("100000L", ex2.get(13).lexeme());

        // Example 3
        List<Token> ex3 = lexer.tokenize("int binary = 0b1010;\nint hexadecimal = 0xFF;\nint octal = 077;");
        assertEquals("0b1010", ex3.get(3).lexeme());
        assertEquals("0xFF", ex3.get(8).lexeme());
        assertEquals("077", ex3.get(13).lexeme());
        assertEquals(TokenType.EOF, ex3.get(15).type());

        // Example 4
        List<Token> ex4 = lexer.tokenize("boolean result = a >= b && a != 0;");
        assertEquals(List.of(
                TokenType.BOOLEAN, TokenType.IDENTIFIER, TokenType.ASSIGN,
                TokenType.IDENTIFIER, TokenType.GREATER_THAN_OR_EQUAL, TokenType.IDENTIFIER,
                TokenType.LOGICAL_AND,
                TokenType.IDENTIFIER, TokenType.NOT_EQUAL, TokenType.INTEGER_LITERAL,
                TokenType.SEMICOLON, TokenType.EOF
        ), ex4.stream().map(Token::type).toList());

        // Example 5
        List<Token> ex5 = lexer.tokenize("String name = \"RKCC\";\nchar letter = 'A';");
        assertEquals(List.of(
                new Token(TokenType.IDENTIFIER, "String", new SourceLocation(1, 1, 0)),
                new Token(TokenType.IDENTIFIER, "name", new SourceLocation(1, 8, 7)),
                new Token(TokenType.ASSIGN, "=", new SourceLocation(1, 13, 12)),
                new Token(TokenType.STRING_LITERAL, "\"RKCC\"", new SourceLocation(1, 15, 14)),
                new Token(TokenType.SEMICOLON, ";", new SourceLocation(1, 21, 20)),
                new Token(TokenType.CHAR, "char", new SourceLocation(2, 1, 22)),
                new Token(TokenType.IDENTIFIER, "letter", new SourceLocation(2, 6, 27)),
                new Token(TokenType.ASSIGN, "=", new SourceLocation(2, 13, 34)),
                new Token(TokenType.CHARACTER_LITERAL, "'A'", new SourceLocation(2, 15, 36)),
                new Token(TokenType.SEMICOLON, ";", new SourceLocation(2, 18, 39)),
                Token.eof(new SourceLocation(2, 19, 40))
        ), ex5);

        // Example 6
        TokenStream ex6 = lexer.tokenizeToStream("int result = (a + b) * 2;\nresult <<= 1;");
        List<TokenType> ex6Types = new java.util.ArrayList<>();
        while (!ex6.isAtEnd()) {
            ex6Types.add(ex6.advance().type());
        }
        ex6Types.add(ex6.peek().type());
        assertEquals(List.of(
                TokenType.INT, TokenType.IDENTIFIER, TokenType.ASSIGN,
                TokenType.LEFT_PAREN, TokenType.IDENTIFIER, TokenType.PLUS, TokenType.IDENTIFIER, TokenType.RIGHT_PAREN,
                TokenType.STAR, TokenType.INTEGER_LITERAL, TokenType.SEMICOLON,
                TokenType.IDENTIFIER, TokenType.LEFT_SHIFT_ASSIGN, TokenType.INTEGER_LITERAL, TokenType.SEMICOLON,
                TokenType.EOF
        ), ex6Types);
        assertEquals(Token.eof(new SourceLocation(2, 14, 39)), ex6.peek());
    }
}
