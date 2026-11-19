# Developer Onboarding Guide — RKCC JavaCompiler

Welcome to **RKCC JavaCompiler**. This guide explains how to set up the repository, navigate the shared compiler contracts, write tests, and develop your assigned compiler module independently without blocking on other team members.

---

## 1. Prerequisites & Project Setup

- **Java Development Kit (JDK):** Java 17 or newer (compiled with `--release 17` compatibility in [pom.xml](../pom.xml))
- **Build Tool:** Apache Maven 3.9+
- **Testing Framework:** JUnit 5 (`org.junit.jupiter:junit-jupiter` `5.12.2`)
- **Version Control:** Git (`origin`: `https://github.com/Krishna2132/JavaCompiler.git`)

Clone and verify your baseline build:

```bash
git clone https://github.com/Krishna2132/JavaCompiler.git
cd JavaCompiler
git status
mvn clean test
```

> **Mandatory Reading First:** Read [AGENTS.md](../AGENTS.md), [ARCHITECTURE.md](ARCHITECTURE.md), [TEAM_CONTRACT.md](TEAM_CONTRACT.md), and [PROGRESS.md](PROGRESS.md) before designing or editing any milestone.

---

## 2. Repository Structure

```text
JavaCompiler/
├── AGENTS.md                           # Mandatory contributor & AI-agent guardrails
├── README.md                           # Project overview & status summary
├── pom.xml                             # Maven build (Java 17, JUnit 5, Surefire)
├── docs/
│   ├── ARCHITECTURE.md                 # Pipeline blueprint, contracts & dependency rules
│   ├── DEVELOPER_GUIDE.md              # This onboarding guide
│   ├── PROGRESS.md                     # Milestone readiness checklist & test results
│   └── TEAM_CONTRACT.md                # Team ownership, actual contract APIs & rules
└── src/
    ├── main/java/rkcc/compiler/
    │   ├── core/                       # AbstractTypeParser, TypeRegistry, AssemblyBackend, AssemblyGenerationContext
    │   ├── lexer/                      # Lexer, Token, TokenType, TokenStream, ListTokenStream, exceptions
    │   ├── diagnostics/                # SourceLocation, Diagnostic*, DiagnosticCollector, DiagnosticFormatter, CompilerException
    │   ├── types/                      # TypeDescriptor, TypeCategory (concrete types pending)
    │   ├── ast/                        # AstNode contract (concrete AST nodes pending)
    │   ├── parser/                     # Package boundary (Jatin)
    │   ├── semantic/                   # Package boundary (Janesh)
    │   ├── codegen/                    # Package boundary (Riya)
    │   └── integration/                # Package boundary (Krishna)
    └── test/java/rkcc/compiler/
        ├── BuildVerificationTest.java
        ├── core/                       # AbstractTypeParserTest, TypeRegistryTest
        ├── diagnostics/                # SourceLocationTest, DiagnosticsTest, DiagnosticIntegrationTest
        ├── lexer/                      # LexerTest, TokenTest, TokenTypeTest, ListTokenStreamTest
        └── integration/                # FoundationTestFixtures, CompilerFoundationIntegrationTest
```

---

## 3. Maven Build & Testing Commands

- **Compile main sources:** `mvn clean compile`
- **Run all unit & foundation integration tests:** `mvn clean test`
- **Run full Maven verification lifecycle:** `mvn clean verify`
- **Build JAR package (with tests):** `mvn clean package`

All tests use **JUnit 5** (`org.junit.jupiter.api.*`) under `src/test/java/rkcc/compiler/<module>/`.

---

## 4. Developing Your Module Independently

You do **not** need to wait for other team members' concrete modules to start building and testing your own module:

1. **Krishna (`core`, `lexer`, `diagnostics`, `integration`):** Foundation contracts, `Lexer`, `TokenStream`, `DiagnosticCollector`, `DiagnosticFormatter`, `AbstractTypeParser`, and `TypeRegistry` are ready for downstream teams to consume.
2. **Jatin (`parser`, `ast`):**
   - Use `new Lexer().tokenizeToStream(source, diagnostics)` to feed real tokens into your parser tests.
   - Implement concrete AST nodes against `rkcc.compiler.ast.AstNode` (implementing `nodeKind()`, `typeDescriptor()`, and `location()`).
   - Delegate type-specific constructs via `TypeRegistry` and `AbstractTypeParser` using test-only parsers until Janesh/Ayush/Ankit land concrete type parsers.
3. **Janesh (`semantic`, `types`):**
   - Implement `TypeDescriptor` for integer types (`byte`, `short`, `int`, `long`, `char` as assigned) and `AbstractTypeParser` subclasses if applicable.
   - Test `SemanticAnalyzer` by feeding test-constructed `AstNode` trees and recording semantic/type errors in `DiagnosticCollector`.
4. **Riya (`codegen`):**
   - Implement `rkcc.compiler.core.AssemblyBackend` in `rkcc.compiler.codegen` (`targetName()`, `emitAssembly(AstNode, TypeDescriptor, AssemblyGenerationContext)`).
   - Test instruction emission using `FoundationTestFixtures`-style `AstNode` and `TypeDescriptor` test doubles without waiting for `Parser` or `SemanticAnalyzer`.
5. **Ayush (`String` type) & Ankit (`boolean` type):**
   - Extend `AbstractTypeParser<T>` with your `TypeDescriptor` (`TypeCategory.STRING` for Ayush, `TypeCategory.BOOLEAN` for Ankit), register into `TypeRegistry`, and test via `Lexer` + `ListTokenStream` + `StubAssemblyBackend` doubles.

---

## 5. Coding & Naming Conventions

- Target **Java 17** (`--release 17`); prefer `record`s and immutable types for data carriers.
- Follow standard Java naming: `PascalCase` classes/interfaces/records, `camelCase` methods/fields, `UPPER_SNAKE_CASE` constants/enums.
- Keep package dependencies strictly acyclic (see [ARCHITECTURE.md](ARCHITECTURE.md#8-module-dependencies)):
  - Never import `parser` or `lexer` internals inside `codegen`.
  - Never emit assembly inside `parser` or `AbstractTypeParser`.
  - Keep all test doubles under `src/test/java/`.
- Validate API preconditions with `IllegalArgumentException` / `IllegalStateException`, and report source-code failures via `DiagnosticCollector` / `CompilerException`.

---

## 6. Git Branching, Commit & Pull Request Guidelines

- **Protected `main`:** Never commit or push directly to `main`, and never force-push shared history.
- **Feature Branches:** Work on your assigned branch (do not create or push remote branches without team coordination):
  - Krishna: `krishna/compiler-core`
  - Jatin: `jatin/parser-ast` *(proposed)*
  - Janesh: `janesh/semantic-types` *(proposed)*
  - Riya: `riya/codegen-x86` *(proposed)*
  - Ayush / Ankit: module-scoped feature branches *(proposed)*
- **Commit Messages (Conventional Commits):**
  - `feat(<scope>): ...` (e.g., `feat(parser): implement arithmetic expression AST`)
  - `test(<scope>): ...`
  - `docs(<scope>): ...`
  - `fix(<scope>): ...`
- **Pull Request Checklist:**
  1. Rebase/sync cleanly with latest shared contract changes (never rewrite shared `main` history).
  2. Confirm only files inside your owned package(s) + your tests/docs were modified.
  3. Confirm `mvn clean verify` passes with **0 failures, 0 errors, 0 skipped tests**.
  4. Run `git diff --check` to ensure zero trailing whitespace or formatting issues.
  5. Update [PROGRESS.md](PROGRESS.md) and reference any shared contract needs in [TEAM_CONTRACT.md](TEAM_CONTRACT.md).
