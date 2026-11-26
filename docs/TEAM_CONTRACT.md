# Team Contract & Integration Rules — RKCC JavaCompiler

This document defines the module boundaries, shared data contracts, branching rules, and testing expectations enabling **Krishna**, **Jatin**, **Janesh**, and **Riya** to work in parallel and integrate cleanly.

> **Status Notice (Milestone 05):** Shared contracts (`SourceLocation`, `TokenType`, `Token`, `TokenStream`, `ListTokenStream`, `UnexpectedTokenException`, `TypeCategory`, `TypeDescriptor`, `AstNode`, `AssemblyGenerationContext`, `AssemblyBackend`) and `AbstractTypeParser` are implemented and verified in Java. Remaining stage contracts (`DiagnosticReporter`, `Lexer`, `Parser`, `SemanticAnalyzer`, `CodeGenerator`, `CompilerPipeline`) will be added in upcoming milestones.

---

## 1. Individual Module Responsibilities

### Krishna — Core Architecture, Lexer, Diagnostics & Integration
- **Packages:** `rkcc.compiler.core`, `rkcc.compiler.lexer`, `rkcc.compiler.diagnostics`, `rkcc.compiler.integration`
- **Deliverables:**
  - Shared stage contracts, `DiagnosticReporter`, `SourceSpan`, `Token`, `TokenStream`, and `AbstractTypeParser` base class.
  - Lexical analyzer transforming Java source text into `TokenStream`.
  - `CompilerPipeline` orchestrating Lexer → Parser → Semantic Analyzer → Assembly Generator.
  - Integration test suite verifying end-to-end compilation.

### Jatin — Parser & Abstract Syntax Tree
- **Packages:** `rkcc.compiler.parser`, `rkcc.compiler.ast`
- **Deliverables:**
  - AST node hierarchy (`CompilationUnit`, declarations, statements, expressions) with `SourceSpan` tracking and visitor support.
  - Parser consuming `TokenStream`, delegating type-specific constructs to registered `AbstractTypeParser` handlers, and emitting syntax diagnostics.
  - Unit tests covering valid grammar structures and syntax error recovery.

### Janesh — Type System & Semantic Analysis
- **Packages:** `rkcc.compiler.types`, `rkcc.compiler.semantic`
- **Deliverables:**
  - Initial integer/integral type system built atop the shared `TypeDescriptor` contract, extensible to all future Java types.
  - Symbol table and lexical/block scope resolution.
  - Semantic validator checking declarations, scope rules, and type compatibility, producing `TypedAstNode` / `TypedCompilationUnit`.
  - Unit tests covering valid semantic resolution and type/scope error diagnostics.

### Riya — x86 Assembly Generation
- **Packages:** `rkcc.compiler.codegen`
- **Deliverables:**
  - Assembly emitter translating `TypedAstNode` / `TypedCompilationUnit` into x86 assembly text (`AssemblyArtifact`).
  - Stack/register allocation strategy for the agreed integral-language subset.
  - Unit tests verifying deterministic assembly emission from mock/stub `TypedAstNode` trees.
  - *Note:* Linux x86-64 with NASM syntax is currently a **proposed** target pending supervisor confirmation.

---

## 2. Proposed Shared Interfaces & Data Exchange

Modules exchange data strictly via the following boundaries:

| Boundary | Producer | Consumer | Exchanged Contract |
| --- | --- | --- | --- |
| Source → Lexer | `integration` | `lexer` | `(String sourceName, String sourceCode)` |
| Lexer → Parser | `lexer` | `parser` | `TokenStream` (yielding immutable `Token` records with `SourceSpan`) |
| Core ↔ Parser | `core` | `parser` | `AbstractTypeParser` / `TypeParserRegistry` |
| Parser → Semantic | `parser` | `semantic` | `AstNode` (`CompilationUnit`) |
| Types ↔ Semantic/Codegen | `types` | `semantic`, `codegen` | `TypeDescriptor`, `TypeCategory` |
| Semantic → Codegen | `semantic` | `codegen` | `TypedAstNode` (`TypedCompilationUnit`) |
| Codegen → Pipeline | `codegen` | `integration` | `AssemblyArtifact` |
| All Stages → Pipeline | All | `integration` | `Diagnostic` via `DiagnosticReporter` |

### Core Contract Summary (Implemented vs. Planned)

**Implemented in Milestones 03–05:**
- `SourceLocation` (`rkcc.compiler.diagnostics`) — immutable 1-based `line`/`column` and 0-based `offset` coordinates with validation and `formatCompact()`
- `TokenType`, `Token`, `TokenStream`, `ListTokenStream`, `UnexpectedTokenException` (`rkcc.compiler.lexer`) — strongly typed `TokenType` enum, immutable `record Token(TokenType type, String lexeme, SourceLocation location)`, full `TokenStream` (`peek()`, `peek(int)`, `advance()`, `previous()`, `check(TokenType)`, `match(TokenType)`, `match(TokenType...)`, `consume(TokenType, String)`, `isAtEnd()`, `position()`), and validated `ListTokenStream` implementation
- `TypeCategory`, `TypeDescriptor` (`rkcc.compiler.types`) — `typeName()`, `category()`, `byteSize()`, `isSigned()`, `isPrimitive()`, `isAssignableTo(TypeDescriptor)`
- `AstNode` (`rkcc.compiler.ast`) — `nodeKind()`, `typeDescriptor()`
- `AssemblyGenerationContext`, `AssemblyBackend` (`rkcc.compiler.core`) — `targetName()`, `emitAssembly(AstNode, TypeDescriptor, AssemblyGenerationContext)`
- `AbstractTypeParser<T extends AstNode>` (`rkcc.compiler.core`) — `getTypeDescriptor()`, `supports(String)`, `supports(TokenStream)`, `requireSupportedType(String)`, `parse(TokenStream)`, `generateAssembly(...)`

**Integration Requirements for Other Team Members:**
- **Krishna (`lexer`):** Concrete `Lexer` scanner and `TokenStream` implementations must produce `rkcc.compiler.lexer.Token` records using `TokenType` and `SourceLocation` and implement `rkcc.compiler.lexer.TokenStream`.
- **Jatin (`parser`, `ast`):** Concrete AST nodes must implement `rkcc.compiler.ast.AstNode` (carrying `SourceLocation` where needed). `Parser` must consume `TokenStream`, inspect `Token.type()` / `TokenStream.match(TokenType)`, and delegate type-specific constructs to registered `AbstractTypeParser` instances.
- **Janesh (`types`, `semantic`):** Concrete integer and future type models must implement `rkcc.compiler.types.TypeDescriptor` using `TypeCategory`.
- **Riya (`codegen`):** Target assembly emitters must implement `rkcc.compiler.core.AssemblyBackend` so `AbstractTypeParser.generateAssembly(...)` and the future `CodeGenerator` integrate cleanly without duplicating codegen logic.

**Planned for Future Milestones:**
See [ARCHITECTURE.md](ARCHITECTURE.md#5-implemented-essential-contracts--proposed-future-signatures) for proposed signatures of `DiagnosticReporter`, `Lexer`, `Parser`, `SemanticAnalyzer`, `CodeGenerator`, and `CompilerPipeline`.

---

## 3. Integration Rules

1. **No Direct Cross-Module Concrete Coupling:**
   - `parser` interacts with `lexer` through `Token` / `TokenStream` contracts, not lexer internal scanner state.
   - `codegen` interacts with `semantic` through `TypedAstNode` and `TypeDescriptor`, never AST parsing internals.
2. **Contract Freeze Protocol:**
   - Once Krishna publishes shared interfaces in `core`, `diagnostics`, and `types` (Milestone 03), breaking signature changes require agreement from all affected module owners before merging.
3. **Error Propagation Rule:**
   - Every stage records errors into `DiagnosticReporter`.
   - `CompilerPipeline` aborts downstream stages after any stage produces `DiagnosticSeverity.ERROR` (or runs only safe stages) so `CodeGenerator` never runs on semantically invalid ASTs.
4. **No Duplicate Implementations:**
   - Do not re-implement `SourceSpan`, `Diagnostic`, or `TypeDescriptor` inside individual feature packages. Reuse the shared contracts.

---

## 4. Branch Ownership & Git Workflow

| Owner | Designated Branch Pattern | Status |
| --- | --- | --- |
| Krishna | `krishna/compiler-core` | Active (Milestones 01–02) |
| Jatin | `jatin/parser-ast` *(proposed)* | Pending start |
| Janesh | `janesh/semantic-types` *(proposed)* | Pending start |
| Riya | `riya/codegen-x86` *(proposed)* | Pending start |
| Integration | `main` | Protected — PR/reviewed merge only |

### Git Rules
- Never commit directly to `main`.
- Never force push or rewrite shared branch history.
- Keep commits atomic, descriptive, and verified with `mvn clean test`.

---

## 5. Testing Expectations

1. **Framework:** JUnit 5 (`org.junit.jupiter.api.*`) exclusively.
2. **Per-Module Isolation:** Each developer must write unit tests for their own package under `src/test/java/rkcc/compiler/<package>/` using lightweight test doubles/mocks for upstream contracts until integration.
3. **Mandatory Coverage Areas:**
   - **Lexer:** Keywords, identifiers, integer literals, operators, whitespace/comments, invalid characters, accurate `SourceSpan`.
   - **Parser/AST:** Valid declarations/expressions, operator precedence, `AbstractTypeParser` delegation, syntax error diagnostics.
   - **Semantic/Types:** Symbol resolution, duplicate/undeclared variable errors, integral type compatibility, `TypedAstNode` accuracy.
   - **Codegen:** Valid assembly generation for integral operations, stack/register consistency, deterministic output formatting.
   - **Integration:** Full `Java Source -> Assembly` golden tests and end-to-end error reporting tests.
4. **Build Gate:** `mvn clean test` must exit `0` with zero failing or skipped tests before opening a merge request or marking a milestone done.
