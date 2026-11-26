# Team Contract & Integration Rules — RKCC JavaCompiler

This document defines module ownership, finalized shared Java contracts, provisional stage boundaries, and testing/integration expectations enabling **Krishna**, **Jatin**, **Janesh**, **Riya**, **Ayush**, and **Ankit** to work independently and integrate cleanly.

> **Status Notice (Milestone 09A):** All shared foundation contracts below are implemented, finalized, and verified (`mvn clean test` & `mvn clean verify`). Provisional stage-driver interfaces (`Parser`, `SemanticAnalyzer`, `TypedAstNode`, `CodeGenerator`, `CompilerPipeline`) remain design proposals awaiting Milestone 09B team integration alignment.

---

## 1. Finalized Shared Contracts vs. Provisional Contracts

### Stable Finalized Contracts (Implemented in Java)

| Package | Implemented Types & Exact Public Signatures |
| --- | --- |
| `rkcc.compiler.diagnostics` | `SourceLocation(int line, int column, int offset)` (`START`, `formatCompact()`)<br>`DiagnosticSeverity` (`ERROR`, `WARNING`, `INFO`, `isError()`, `isWarning()`, `isInfo()`)<br>`CompilerPhase` (`LEXICAL_ANALYSIS`, `SYNTAX_ANALYSIS`, `SEMANTIC_ANALYSIS`, `TYPE_CHECKING`, `CODE_GENERATION`, `INTERNAL`, `displayName()`)<br>`DiagnosticCode` (`code()`, `defaultPhase()`, `defaultDescription()`, `fromCode(String)`)<br>`Diagnostic(DiagnosticCode, DiagnosticSeverity, CompilerPhase, String, Optional<SourceLocation>, Optional<String>)` + `error(...)`, `warning(...)`, `info(...)`<br>`DiagnosticCollector` (`report`, `reportError`, `reportWarning`, `reportInfo`, `getDiagnostics`, `getErrors`, `getWarnings`, `getInfoDiagnostics`, `getBySeverity`, `size`, `countBySeverity`, `hasErrors`, `hasWarnings`, `isEmpty`, `clear`)<br>`DiagnosticFormatter` (`format(Diagnostic)`, `format(Diagnostic, String)`, `formatAll(Collection<Diagnostic>, String)`)<br>`CompilerException` (`getDiagnostic()`, `getCode()`, `getPhase()`, `getSourceLocation()`) |
| `rkcc.compiler.types` | `TypeCategory` (`INTEGRAL`, `FLOATING_POINT`, `BOOLEAN`, `CHAR`, `STRING`, `ARRAY`, `REFERENCE`, `VOID`, `UNKNOWN`, `isPrimitiveCategory()`, `isReferenceCategory()`, `isVoid()`)<br>`TypeDescriptor` (`typeName()`, `category()`, `byteSize()`, `isSigned()`, `isPrimitive()`, `isAssignableTo(TypeDescriptor)`) |
| `rkcc.compiler.ast` | `AstNode` (`nodeKind()`, `typeDescriptor()`, `default Optional<SourceLocation> location()`) |
| `rkcc.compiler.lexer` | `TokenType` (`fixedLexeme()`, `category()`, `isPrimitiveTypeKeyword()`, `isLiteral()`, `isArithmeticOperator()`, `isAssignmentOperator()`, `isBitwiseOperator()`, `isComparisonOperator()`, `isLogicalOperator()`, `fromKeyword(String)`)<br>`Token(TokenType type, String lexeme, SourceLocation location)` (`Token.eof(SourceLocation)`, `is(TokenType)`, `isPrimitiveTypeKeyword()`)<br>`TokenStream` (`peek()`, `peek(int)`, `advance()`, `previous()`, `check(TokenType)`, `match(TokenType)`, `match(TokenType...)`, `consume(TokenType, String)`, `consume(TokenType, String, DiagnosticCollector)`, `isAtEnd()`, `position()`)<br>`ListTokenStream(List<Token>)`<br>`Lexer` (`tokenize(String)`, `tokenize(String, DiagnosticCollector)`, `tokenizeToStream(String)`, `tokenizeToStream(String, DiagnosticCollector)`)<br>`LexicalException`, `UnexpectedTokenException` (both extend `CompilerException`) |
| `rkcc.compiler.core` | `AssemblyGenerationContext` (`getTargetArchitecture()`, `getSectionHint()`, `getAttributes()`)<br>`AssemblyBackend` (`targetName()`, `emitAssembly(AstNode, TypeDescriptor, AssemblyGenerationContext)`)<br>`AbstractTypeParser<T extends AstNode>` (`getTypeDescriptor()`, `getSupportedTypeName()`, `getSupportedTypeNames()`, `supports(String)`, `supports(TokenStream)`, `requireSupportedType(String)`, `parse(TokenStream)`, `getAssemblyBackend()`, `generateAssembly(T, AssemblyGenerationContext)`, `generateAssembly(T, AssemblyGenerationContext, AssemblyBackend)`)<br>`TypeRegistry` (`register(AbstractTypeParser<?>)`, `register(String, AbstractTypeParser<?>)`, `register(Collection<String>, AbstractTypeParser<?>)`, `register(AbstractTypeParser<?>, DiagnosticCollector)`, `resolve(String)`, `resolve(String, DiagnosticCollector)`, `resolveRequired(String)`, `resolve(Token)`, `resolve(Token, DiagnosticCollector)`, `supports(String)`, `supports(Token)`, `unregister(String)`, `getRegisteredTypes()`, `getRegisteredParsers()`, `size()`, `isEmpty()`) |

### Provisional Stage Interfaces (Pending Team Alignment in Milestone 09B)

The following stage driver contracts remain architectural proposals in [ARCHITECTURE.md](ARCHITECTURE.md#5-implemented-contracts--provisional-future-signatures) and are **not yet Java files**:
- `Parser` (`rkcc.compiler.parser`)
- `TypedAstNode`, `SemanticAnalyzer` (`rkcc.compiler.semantic`)
- `AssemblyArtifact`, `CodeGenerator` (`rkcc.compiler.codegen`)
- `CompilationResult`, `CompilerPipeline` (`rkcc.compiler.integration`)

---

## 2. Detailed Team Module Contracts

### Krishna — Compiler Core, Lexer, Diagnostics & Integration
- **Expected Packages:** `rkcc.compiler.core`, `rkcc.compiler.lexer`, `rkcc.compiler.diagnostics`, `rkcc.compiler.integration`
- **Responsibilities:** Shared architecture, `AbstractTypeParser`, `TypeRegistry`, `Token`, `TokenType`, `TokenStream`, `ListTokenStream`, `Lexer`, diagnostics (`Diagnostic*`, `SourceLocation`, `CompilerException`), future `CompilerPipeline` and CLI.
- **Expected Input / Output:** Source `String` → `List<Token>` / `TokenStream` + `DiagnosticCollector` diagnostics.
- **Dependencies:** `diagnostics` (root), `types`, `ast` (`AstNode` contract).
- **Testing Requirements:** Unit tests for lexer, token stream, diagnostics, registry, `AbstractTypeParser`, and foundation integration tests (`src/test/java/rkcc/compiler/integration/`).

### Jatin — Parser & Abstract Syntax Tree
- **Expected Packages:** `rkcc.compiler.parser`, `rkcc.compiler.ast`
- **Responsibilities:** Syntax analysis, variable declarations, assignment statements, arithmetic expressions, operator precedence, AST construction.
- **Shared Interfaces Consumed/Implemented:** `TokenStream`, `Token`, `TokenType`, `TypeRegistry`, `AbstractTypeParser`, `AstNode`, `DiagnosticCollector`, `DiagnosticCode` (`SYN001`–`SYN004`), `SourceLocation`.
- **Required Method Signatures (Actual Contracts Used):**
  - `TokenStream.peek()`, `TokenStream.advance()`, `TokenStream.match(TokenType...)`, `TokenStream.consume(TokenType, String, DiagnosticCollector)`
  - `TypeRegistry.resolve(Token, DiagnosticCollector)`
  - `AstNode`: `nodeKind()`, `typeDescriptor()`, `location()`
- **Expected Input / Output:** `TokenStream` (+ `TypeRegistry` & `DiagnosticCollector`) → root `AstNode` tree (carrying `SourceLocation` on nodes).
- **Error-Handling Requirements:** Report syntax errors (`SYN_UNEXPECTED_TOKEN`, `SYN_EXPECTED_TOKEN_MISSING`, `SYN_UNEXPECTED_END_OF_INPUT`, `SYN_INVALID_EXPRESSION`) via `DiagnosticCollector` / `UnexpectedTokenException` without swallowing `CompilerException`s.
- **Dependencies:** `diagnostics`, `lexer`, `core`, `ast`, `types`.
- **Testing Requirements:** JUnit 5 tests under `src/test/java/rkcc/compiler/parser/` and `ast/` covering valid grammar, precedence, `AbstractTypeParser` delegation, and syntax error reporting.
- **Integration Example:**
  ```java
  Optional<AbstractTypeParser<?>> typeParser = registry.resolve(tokens.peek(), diagnostics);
  AstNode declType = typeParser.map(p -> p.parse(tokens)).orElse(null);
  Token name = tokens.consume(TokenType.IDENTIFIER, "Expected variable name", diagnostics);
  ```

### Janesh — Semantic Analysis & Integer Type System
- **Expected Packages:** `rkcc.compiler.semantic`, `rkcc.compiler.types`
- **Responsibilities:** Integer type representation (`byte`, `short`, `int`, `long`, `char`), symbol table, type checking, numeric range validation, type promotion, type conversion, semantic validation.
- **Shared Interfaces Consumed/Implemented:** `TypeDescriptor`, `TypeCategory`, `AbstractTypeParser` (if providing integral type parser), `AstNode`, `DiagnosticCollector`, `DiagnosticCode` (`SEM001`–`SEM006`), `SourceLocation`.
- **Required Method Signatures (Actual Contracts Used):**
  - Implement `TypeDescriptor`: `typeName()`, `category()`, `byteSize()`, `isSigned()`, `isPrimitive()`, `isAssignableTo(TypeDescriptor)`
  - Inspect `AstNode`: `nodeKind()`, `typeDescriptor()`, `location()`
  - Report via `DiagnosticCollector.reportError(DiagnosticCode, String, SourceLocation)`
- **Expected Input / Output:** `AstNode` tree + `DiagnosticCollector` → validated typed AST / symbol metadata.
- **Error-Handling Requirements:** Report `SEM_UNDEFINED_VARIABLE`, `SEM_DUPLICATE_DECLARATION`, `TYP_TYPE_MISMATCH`, `TYP_INVALID_CONVERSION`, `TYP_UNSUPPORTED_TYPE`, `TYP_INTEGER_OUT_OF_RANGE` with `AstNode.location().orElse(null)`.
- **Dependencies:** `diagnostics`, `types`, `ast`, `core`.
- **Testing Requirements:** JUnit 5 tests under `src/test/java/rkcc/compiler/types/` and `semantic/` verifying integral range bounds, promotions, scope rules, and semantic error diagnostics.
- **Integration Example:**
  ```java
  if (!lhsType.isAssignableTo(rhsType)) {
      diagnostics.reportError(DiagnosticCode.TYP_TYPE_MISMATCH,
              "Cannot assign " + rhsType.typeName() + " to " + lhsType.typeName(),
              node.location().orElse(null));
  }
  ```

### Riya — x86 Assembly Backend
- **Expected Package:** `rkcc.compiler.codegen`
- **Responsibilities:** Assembly generation, register allocation, memory/stack allocation, instruction selection, arithmetic & bitwise lowering, assembly output. *(Linux x86-64 NASM syntax remains proposed pending supervisor confirmation.)*
- **Shared Interfaces Consumed/Implemented:** `AssemblyBackend`, `AssemblyGenerationContext`, `AstNode`, `TypeDescriptor`, `TypeCategory`, `DiagnosticCollector`, `DiagnosticCode` (`GEN001`–`GEN003`).
- **Required Method Signatures (Actual Contracts Used):**
  - Implement `AssemblyBackend`: `String targetName()` and `String emitAssembly(AstNode node, TypeDescriptor typeDescriptor, AssemblyGenerationContext context)`
  - Read `AssemblyGenerationContext.getTargetArchitecture()`, `getSectionHint()`, `getAttributes()`
- **Expected Input / Output:** Validated `AstNode` + `TypeDescriptor` + `AssemblyGenerationContext` → non-blank x86 assembly `String`.
- **Error-Handling Requirements:** Report `GEN_UNSUPPORTED_OPERATION`, `GEN_INVALID_ASSEMBLY_OPERAND`, `GEN_UNSUPPORTED_TARGET` via `DiagnosticCollector` / `CompilerException` rather than emitting invalid instructions.
- **Dependencies:** `diagnostics`, `core`, `types`, `ast` (and `semantic` when `TypedAstNode` is introduced). Must **never** depend on `lexer` or `parser`.
- **Testing Requirements:** JUnit 5 tests under `src/test/java/rkcc/compiler/codegen/` using test `AstNode` and `TypeDescriptor` doubles.
- **Integration Example:**
  ```java
  public final class X86AssemblyBackend implements AssemblyBackend {
      @Override public String targetName() { return "x86_64-nasm-proposed"; }
      @Override public String emitAssembly(AstNode node, TypeDescriptor type, AssemblyGenerationContext ctx) {
          // Lower validated node using type.byteSize() and type.isSigned()
          return "...";
      }
  }
  ```

### Ayush — String Type Module
- **Expected Package:** `rkcc.compiler.types` (or dedicated subpackage coordinated via `TypeRegistry`)
- **Responsibilities:** `String` type parsing (`AbstractTypeParser`), `String` descriptor (`TypeCategory.STRING`, `isPrimitive() == false`), `String`-specific validation and backend delegation.
- **Shared Interfaces Consumed/Implemented:** `AbstractTypeParser<T>`, `TypeDescriptor`, `TypeCategory.STRING`, `TokenStream`, `TokenType.IDENTIFIER` (`"String"`), `TokenType.STRING_LITERAL`, `TypeRegistry`, `AssemblyBackend`, `DiagnosticCollector`.
- **Expected Input / Output:** `TokenStream` positioned at `"String"` / `STRING_LITERAL` → `AstNode` (`location()`, `typeDescriptor()`) + delegated assembly via `generateAssembly(...)`.
- **Error-Handling Requirements:** Report `TYP_TYPE_MISMATCH` / `TYP_UNSUPPORTED_TYPE` via `DiagnosticCollector`.
- **Testing Requirements:** JUnit 5 tests verifying `TypeRegistry.register(stringParser)` and `registry.resolve(new Token(TokenType.IDENTIFIER, "String", loc))`.
- **Integration Example:**
  ```java
  registry.register(stringTypeParser);
  Optional<AbstractTypeParser<?>> p = registry.resolve(new Token(TokenType.IDENTIFIER, "String", loc));
  ```

### Ankit — Boolean Type Module
- **Expected Package:** `rkcc.compiler.types` (or dedicated subpackage coordinated via `TypeRegistry`)
- **Responsibilities:** `boolean` type parsing (`AbstractTypeParser`), logical operations (`&&`, `||`, `!`), boolean expressions, boolean-specific validation and backend delegation.
- **Shared Interfaces Consumed/Implemented:** `AbstractTypeParser<T>`, `TypeDescriptor`, `TypeCategory.BOOLEAN`, `TokenStream`, `TokenType.BOOLEAN` / `TRUE` / `FALSE`, `TypeRegistry`, `AssemblyBackend`, `DiagnosticCollector`.
- **Expected Input / Output:** `TokenStream` positioned at `boolean` / boolean expression → `AstNode` (`location()`, `typeDescriptor()`) + delegated assembly via `generateAssembly(...)`.
- **Error-Handling Requirements:** Report `TYP_TYPE_MISMATCH` / `TYP_INVALID_CONVERSION` via `DiagnosticCollector`.
- **Testing Requirements:** JUnit 5 tests verifying `TypeRegistry.register(booleanParser)` and `registry.resolve(new Token(TokenType.BOOLEAN, "boolean", loc))`.
- **Integration Example:**
  ```java
  registry.register(booleanTypeParser);
  Optional<AbstractTypeParser<?>> p = registry.resolve(new Token(TokenType.BOOLEAN, "boolean", loc));
  ```

---

## 3. Data Exchange & Integration Rules

| Boundary | Producer | Consumer | Actual Exchanged Contract |
| --- | --- | --- | --- |
| Source → Lexer | Caller / Pipeline | `lexer` | `String source` (+ optional `DiagnosticCollector`) |
| Lexer → Parser | `lexer` | `parser` | `TokenStream` (`ListTokenStream` yielding `Token(TokenType, String, SourceLocation)`) |
| Core ↔ Parser | `core` | `parser` | `TypeRegistry`, `AbstractTypeParser<T extends AstNode>` |
| Parser → Semantic | `parser` | `semantic` | `AstNode` (`nodeKind()`, `typeDescriptor()`, `location()`) |
| Types ↔ All | `types` | `core`, `ast`, `semantic`, `codegen` | `TypeDescriptor`, `TypeCategory` |
| Core ↔ Codegen | `core` | `codegen` | `AssemblyBackend`, `AssemblyGenerationContext` |
| All → Diagnostics | All | Caller / Pipeline | `Diagnostic` via `DiagnosticCollector` & `DiagnosticFormatter` |

1. **Acyclic Dependencies:** `diagnostics` and `types` sit at the bottom with zero internal compiler dependencies. `codegen` must never depend on `lexer` or `parser`.
2. **Contract Compatibility:** Any breaking change to finalized shared contracts requires alignment across affected owners before merging.
3. **No Duplicate Models:** Never duplicate `SourceLocation`, `Token`, `TokenStream`, `AstNode`, `TypeDescriptor`, `Diagnostic`, or `AbstractTypeParser`.

---

## 4. Branch Ownership & Testing Gate

- Active branch: `krishna/compiler-core` (Milestones 01–09A).
- Protected `main`: never commit or push directly to `main`, never force-push shared history.
- Mandatory build gate before PR/commit: `mvn clean verify` (and `git diff --check`) passing with zero failures, zero errors, and zero skipped tests.
