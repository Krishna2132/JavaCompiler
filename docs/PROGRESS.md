# Project Progress — RKCC JavaCompiler

## 1. Completed Milestones

### Milestone 01 — Maven Project Initialization
- **Status:** Completed and verified
- **Commit:** `e3d16e0` (`build: initialize Maven JavaCompiler project`)
- **Deliverables:**
  - Maven `pom.xml` (`rkcc.compiler:javacompiler:0.1.0-SNAPSHOT`, Java `17` release compatibility, UTF-8 encoding, JUnit Jupiter `5.12.2`, `maven-compiler-plugin` `3.14.1`, `maven-surefire-plugin` `3.5.6`).
  - Standard Maven directories (`src/main/java/rkcc/compiler/`, `src/test/java/rkcc/compiler/`).
  - Updated `.gitignore` and initial `README.md`.
  - Minimal `BuildVerificationTest.java`.

### Milestone 02 — Common Compiler Architecture
- **Status:** Completed and verified
- **Commit:** `c0f0a5f` (`feat: establish modular compiler architecture`)
- **Deliverables:**
  - Root [AGENTS.md](../AGENTS.md) establishing project context, team ownership, Git rules, and testing standards.
  - Nine modular Java packages documented via `package-info.java`.
  - Architecture blueprint in [ARCHITECTURE.md](ARCHITECTURE.md), collaboration rules in [TEAM_CONTRACT.md](TEAM_CONTRACT.md), progress tracking in [PROGRESS.md](PROGRESS.md), and updated [README.md](../README.md).

### Milestone 03 — Abstract Type Parser & Essential Shared Contracts
- **Status:** Completed and verified
- **Commits:**
  - `2d6148f` — `feat: define essential compiler contracts`
  - `87509dd` — `feat: implement abstract type parser`
  - `c16fe87` — `test: validate abstract type parser behavior`
  - `9057086` — `docs: document abstract parser integration`
- **Deliverables:**
  - Essential shared contracts: `TypeCategory`, `TypeDescriptor`, `Token`, `TokenStream`, `AstNode`, `AssemblyGenerationContext`, `AssemblyBackend`.
  - Reusable generic abstract base class `AbstractTypeParser<T extends AstNode>` in `rkcc.compiler.core` with input/output validation and backend delegation.
  - JUnit 5 tests in [AbstractTypeParserTest.java](../src/test/java/rkcc/compiler/core/AbstractTypeParserTest.java).

### Milestone 04 — Token Model & Source Location
- **Status:** Completed and verified
- **Commit:** `bfbd94f` (`feat(lexer): implement token model and source location tracking`)
- **Deliverables:**
  - `SourceLocation` record (`rkcc.compiler.diagnostics`) tracking 1-based `line`, 1-based `column`, and 0-based `offset` with constructor validation.
  - `TokenType` enum (`rkcc.compiler.lexer`) covering Java primitive keywords (`String` treated as `IDENTIFIER`), identifiers, literals, arithmetic, assignment, bitwise, comparison, logical, increment/decrement, delimiters, and `EOF`.
  - Evolved `Token` (`rkcc.compiler.lexer`) into an immutable `record Token(TokenType type, String lexeme, SourceLocation location)` and updated `TokenStream.match(TokenType)` and `AbstractTypeParserTest`.
  - JUnit 5 unit tests in `SourceLocationTest`, `TokenTypeTest`, `TokenTest`, and updated `AbstractTypeParserTest`.

### Milestone 05 — Token Stream Implementation
- **Status:** Completed and verified
- **Commit:** `5e95aae` (`feat(lexer): implement token stream with lookahead and consumption`)
- **Deliverables:**
  - Expanded `TokenStream` (`rkcc.compiler.lexer`) with `previous()`, `check(TokenType)`, `match(TokenType...)`, `consume(TokenType, String)`, and `position()`, preserving `peek()`, `peek(int)`, `advance()`, `isAtEnd()`, and `match(TokenType)`.
  - Implemented `ListTokenStream` (`rkcc.compiler.lexer`) with defensive list copying, strict non-empty single-terminal-EOF validation, safe repeated EOF reads, and `UnexpectedTokenException` for expected-token errors.
  - Added JUnit 5 tests and `int a = 10 + 20;` example in [ListTokenStreamTest.java](../src/test/java/rkcc/compiler/lexer/ListTokenStreamTest.java).

### Milestone 06 — Java Lexer Implementation
- **Status:** Completed and verified
- **Deliverables:**
  - Implemented `Lexer` and `LexicalException` (`rkcc.compiler.lexer`) with `tokenize(String)` and `tokenizeToStream(String)`.
  - Recognizes primitive type keywords (`byte`..`void`), `true`/`false`/`null`, Java identifiers (`String` treated as `IDENTIFIER`), decimal/binary/octal/hex/floating-point/scientific numeric literals with underscores and suffixes, string/char literals with escape validation, longest-match operators, delimiters, whitespace, CRLF/LF/CR newlines, and `//`, `/* */`, `/** */` comments.
  - Verified with comprehensive JUnit 5 tests and 6 integration examples in [LexerTest.java](../src/test/java/rkcc/compiler/lexer/LexerTest.java).

---

## 2. Pending Milestones

| Milestone | Scope | Primary Owner(s) | Status |
| --- | --- | --- | --- |
| **Milestone 07** | Compiler diagnostics (`DiagnosticReporter`, `Diagnostic`, `SourceSpan`) + unit tests | Krishna | Pending |
| **Milestone 08** | Concrete AST nodes and `Parser` with `AbstractTypeParser` integration + unit tests | Jatin | Pending |
| **Milestone 09** | Integer type system (`int` etc.) and `SemanticAnalyzer` producing `TypedAstNode` + unit tests | Janesh | Pending |
| **Milestone 10** | x86 assembly `AssemblyBackend` / `CodeGenerator` implementation + unit tests | Riya | Pending |
| **Milestone 11** | End-to-end `CompilerPipeline` wiring and integration test suite | Krishna (+ Team) | Pending |
| **Future** | Extended Java types (`byte`, `short`, `long`, `char`, `float`, `double`, `boolean`, `String`, arrays, reference types) | Team | Planned |

---

## 3. Verified Test Results

- **Milestone 01 (`e3d16e0`):** `mvn clean test` -> `BUILD SUCCESS` (`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`)
- **Milestone 02 (`c0f0a5f`):** `mvn clean test` -> `BUILD SUCCESS` (`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`)
- **Milestone 03 (`9057086`):** `mvn clean test` -> `BUILD SUCCESS` (`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`)
- **Milestone 04 (`bfbd94f`):** `mvn clean test` -> `BUILD SUCCESS` (`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`)
- **Milestone 05 (`5e95aae`):** `mvn clean test` -> `BUILD SUCCESS` (`Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`)
- **Milestone 06:**
  - Command: `mvn clean test`
  - Outcome: `BUILD SUCCESS` (`Tests run: 39, Failures: 0, Errors: 0, Skipped: 0` — `BuildVerificationTest`: 1, `AbstractTypeParserTest`: 9, `SourceLocationTest`: 3, `LexerTest`: 9, `ListTokenStreamTest`: 9, `TokenTest`: 5, `TokenTypeTest`: 3)

---

## 4. Known Limitations

- Concrete compiler stages (`DiagnosticReporter`, `Parser`, concrete `AstNode`s, `SemanticAnalyzer`, concrete `TypeDescriptor` implementations, `AssemblyBackend` / `CodeGenerator`, and `CompilerPipeline`) are not implemented yet.
- None of the Java data types (`byte`, `short`, `int`, `long`, `char`, `float`, `double`, `boolean`, `String`, arrays, reference types) have concrete semantic/codegen handlers yet.
- `Lexer` currently supports the lexical subset defined in `TokenType` (control-flow/OOP keywords such as `class`, `if`, `else`, `while`, `for`, `return` are tokenized as `IDENTIFIER` until added to `TokenType`); hexadecimal floating-point literals and text blocks (`"""`) are explicitly rejected.
- `Lexer` throws `LexicalException` on the first lexical error rather than collecting multiple errors via `DiagnosticReporter` (planned for Milestone 07).

---

## 5. Unresolved Technical Decisions

The following decisions must be confirmed with the supervisor and team before implementing later milestones:

1. **Target x86 Architecture & Assembly Syntax:** Confirm whether the backend target is **Linux x86-64 with NASM syntax** (currently proposed) or an alternative (e.g., 32-bit x86, Windows x64, GAS/AT&T syntax).
2. **Exact Initial Integral-Language Subset Grammar:** Finalize which constructs belong in the first executable milestone (e.g., standalone statements vs. minimal `class`/`main` wrapper, supported integer operators, variable declarations, control flow such as `if`/`while`, and `return`/exit codes).
3. **Program Entry & Runtime Output Mechanism:** Decide how compiled assembly returns results or prints integers (e.g., process exit status via syscall, `printf`/libc linking, or custom runtime helper).
4. **Integer Bit-Width & Overflow Semantics:** Confirm whether initial `int` lowers to 32-bit (`eax`/`dword`) or 64-bit (`rax`/`qword`) registers/stack slots and how signed overflow or division by zero is handled.
5. **TokenKind Representation:** Decide whether `Token.kind()` uses a centralized `enum TokenType` or extensible token descriptor interface to support future keywords/types cleanly.
6. **AST Mutability vs. Immutability:** Confirm whether `TypedAstNode` wraps immutable syntactic `AstNode` instances or creates a separate typed IR tree.
