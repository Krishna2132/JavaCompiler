# Project Progress — RKCC JavaCompiler

## 1. Completed Milestones

### Milestone 01 — Maven Project Initialization
- **Status:** Completed and verified
- **Commit:** `393a398` (`build: initialize Maven JavaCompiler project`)
- **Deliverables:**
  - Maven `pom.xml` (`rkcc.compiler:javacompiler:0.1.0-SNAPSHOT`, Java `17` release compatibility, UTF-8 encoding, JUnit Jupiter `5.12.2`, `maven-compiler-plugin` `3.14.1`, `maven-surefire-plugin` `3.5.6`).
  - Standard Maven directories (`src/main/java/rkcc/compiler/`, `src/test/java/rkcc/compiler/`).
  - Updated `.gitignore` and initial `README.md`.
  - Minimal `BuildVerificationTest.java`.

### Milestone 02 — Common Compiler Architecture
- **Status:** Completed (pending final commit verification in current milestone)
- **Deliverables:**
  - Root [AGENTS.md](../AGENTS.md) establishing project context, team ownership, Git rules, and testing standards.
  - Nine modular Java packages documented via `package-info.java`:
    - `rkcc.compiler.core`
    - `rkcc.compiler.lexer`
    - `rkcc.compiler.parser`
    - `rkcc.compiler.ast`
    - `rkcc.compiler.semantic`
    - `rkcc.compiler.types`
    - `rkcc.compiler.codegen`
    - `rkcc.compiler.integration`
    - `rkcc.compiler.diagnostics`
  - Architecture blueprint in [ARCHITECTURE.md](ARCHITECTURE.md), including stage specifications, Mermaid pipeline diagram, `AbstractTypeParser` contract design, proposed shared interface signatures, and Java type extensibility roadmap.
  - Collaboration rules in [TEAM_CONTRACT.md](TEAM_CONTRACT.md).
  - Progress tracking in [PROGRESS.md](PROGRESS.md) and updated [README.md](../README.md).

---

## 2. Pending Milestones

| Milestone | Scope | Primary Owner(s) | Status |
| --- | --- | --- | --- |
| **Milestone 03** | Shared core contracts (`diagnostics`, `core`, `types` interfaces), `AbstractTypeParser.java`, and `Lexer` + unit tests | Krishna | Pending |
| **Milestone 04** | Concrete AST nodes and `Parser` with `AbstractTypeParser` integration + unit tests | Jatin | Pending |
| **Milestone 05** | Integer type system and `SemanticAnalyzer` producing `TypedAstNode` + unit tests | Janesh | Pending |
| **Milestone 06** | x86 assembly `CodeGenerator` backend + unit tests | Riya | Pending |
| **Milestone 07** | End-to-end `CompilerPipeline` wiring and integration test suite | Krishna (+ Team) | Pending |
| **Future** | Extended Java types (`byte`, `short`, `long`, `char`, `float`, `double`, `boolean`, `String`, arrays, reference types) | Team | Planned |

---

## 3. Verified Test Results

- **Milestone 01 (`393a398`):**
  - Command: `mvn clean test`
  - Outcome: `BUILD SUCCESS` (`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` — `rkcc.compiler.BuildVerificationTest`)
- **Milestone 02:**
  - Verified with `mvn clean test` (all 9 `package-info.java` declarations compile cleanly with Java 17 target, and `BuildVerificationTest` passes: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`).

---

## 4. Known Limitations

- No compiler stages (`lexer`, `parser`, `ast`, `semantic`, `types`, `codegen`, `integration`, `diagnostics`) are implemented yet; only `package-info.java` boundaries exist in `src/main/java/`.
- `AbstractTypeParser.java` is designed in [ARCHITECTURE.md](ARCHITECTURE.md) but intentionally not implemented yet.
- None of the Java data types (`byte`, `short`, `int`, `long`, `char`, `float`, `double`, `boolean`, `String`, arrays, reference types) are implemented yet.
- Automated test coverage currently consists only of `BuildVerificationTest.java`.

---

## 5. Unresolved Technical Decisions

The following decisions must be confirmed with the supervisor and team before implementing later milestones:

1. **Target x86 Architecture & Assembly Syntax:** Confirm whether the backend target is **Linux x86-64 with NASM syntax** (currently proposed) or an alternative (e.g., 32-bit x86, Windows x64, GAS/AT&T syntax).
2. **Exact Initial Integral-Language Subset Grammar:** Finalize which constructs belong in the first executable milestone (e.g., standalone statements vs. minimal `class`/`main` wrapper, supported integer operators, variable declarations, control flow such as `if`/`while`, and `return`/exit codes).
3. **Program Entry & Runtime Output Mechanism:** Decide how compiled assembly returns results or prints integers (e.g., process exit status via syscall, `printf`/libc linking, or custom runtime helper).
4. **Integer Bit-Width & Overflow Semantics:** Confirm whether initial `int` lowers to 32-bit (`eax`/`dword`) or 64-bit (`rax`/`qword`) registers/stack slots and how signed overflow or division by zero is handled.
5. **TokenKind Representation:** Decide whether `Token.kind()` uses a centralized `enum TokenType` or extensible token descriptor interface to support future keywords/types cleanly.
6. **AST Mutability vs. Immutability:** Confirm whether `TypedAstNode` wraps immutable syntactic `AstNode` instances or creates a separate typed IR tree.
