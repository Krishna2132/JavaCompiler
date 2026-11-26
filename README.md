# JavaCompiler

Modular Java-to-x86 assembly compiler infrastructure for RKCC.

## Project Introduction

`JavaCompiler` (`rkcc.compiler:javacompiler`) is a modular compiler project built in Java for RKCC. Its core design centers on an extensible `AbstractTypeParser` and contract-driven pipeline architecture that maps Java language constructs and data types to x86 assembly output:

```text
Java Source Code -> Lexer -> Token Stream -> Parser -> AST -> Semantic Analyzer -> Typed AST -> Assembly Generator
```

## Project Objective

- Establish a clean, modular compiler pipeline encompassing lexical analysis, extensible type parsing, syntactic AST construction, semantic validation, and x86 assembly generation.
- Enable four developers (**Krishna**, **Jatin**, **Janesh**, and **Riya**) to build their modules independently against shared contracts.
- Support reliable automated unit testing with JUnit 5 and reproducible builds with Maven.

## Documentation

- [AGENTS.md](AGENTS.md) — Project context, development workflow, Git rules, and module ownership boundaries (read before implementing any milestone).
- [docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md) — Setup, build/test commands, module onboarding guide, and Git/PR workflow.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — Compiler pipeline diagram, finalized contracts, dependency graph, and type extensibility roadmap.
- [docs/TEAM_CONTRACT.md](docs/TEAM_CONTRACT.md) — Team responsibilities (Krishna, Jatin, Janesh, Riya, Ayush, Ankit), actual shared APIs, and integration rules.
- [docs/PROGRESS.md](docs/PROGRESS.md) — Foundation readiness checklist, completed/pending milestones, verified test results, and unresolved decisions.

## Package Structure & Ownership

| Package | Responsibility | Owner |
| --- | --- | --- |
| `rkcc.compiler.core` | Core stage contracts and planned `AbstractTypeParser` | Krishna |
| `rkcc.compiler.lexer` | Lexical analysis and token stream generation | Krishna |
| `rkcc.compiler.diagnostics` | Shared diagnostics, severities, and source spans | Krishna |
| `rkcc.compiler.integration` | End-to-end compiler pipeline orchestration | Krishna |
| `rkcc.compiler.parser` | Syntactic analysis and type-parser coordination | Jatin |
| `rkcc.compiler.ast` | AST node contracts and structural representations | Jatin |
| `rkcc.compiler.semantic` | Symbol tables, semantic validation, and typed AST | Janesh |
| `rkcc.compiler.types` | Extensible type system (initial integral subset first) | Janesh *(with Krishna on shared descriptors)* |
| `rkcc.compiler.codegen` | Target x86 assembly generation *(Linux x86-64 NASM proposed)* | Riya |

## Development Prerequisites

- **Java Development Kit (JDK):** Java 17 or newer (built with Java 17 release compatibility)
- **Build Tool:** Apache Maven 3.9+
- **Version Control:** Git

## Build & Test Instructions

From the repository root (`JavaCompiler`):

```bash
mvn clean compile
mvn clean test
mvn clean package
```

## Current Project Status

- **Milestone 01 — Maven Project Initialization:** Completed (`e3d16e0`).
- **Milestone 02 — Common Compiler Architecture:** Completed (`c0f0a5f`).
- **Milestone 03 — Abstract Type Parser & Essential Shared Contracts:** Completed (`2d6148f`, `87509dd`, `c16fe87`, `9057086`).
- **Milestone 04 — Token Model & Source Location:** Completed (`bfbd94f`).
- **Milestone 05 — Token Stream Implementation:** Completed (`5e95aae`).
- **Milestone 06 — Java Lexer Implementation:** Completed (`4448d80`).
- **Milestone 07 — Compiler Diagnostics & Error Handling:** Completed (`d6e6ea9`).
- **Milestone 08 — Type Registry & Type Parser Management:** Completed (`7680364`).
- **Milestone 09A — Shared Compiler Contracts & Foundation Integration Testing:** Completed.
  - Finalized shared contracts (`AstNode.location()`, `AssemblyGenerationContext` attribute validation, updated package Javadocs) without creating speculative production stubs.
  - Added reusable test fixtures ([FoundationTestFixtures.java](src/test/java/rkcc/compiler/integration/FoundationTestFixtures.java)), 6 foundation integration workflow tests ([CompilerFoundationIntegrationTest.java](src/test/java/rkcc/compiler/integration/CompilerFoundationIntegrationTest.java)), and [docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md).
  - Verified with `mvn clean test` and `mvn clean verify`: 62 tests passing (`56` unit tests + `6` foundation integration tests, `0` failures).
- **Pending (Milestone 09B & Future):** `Parser` & concrete AST nodes (Jatin), `SemanticAnalyzer` & integer type system (Janesh), `String` type module (Ayush), `boolean` type module (Ankit), x86 assembly backend (Riya), `CompilerPipeline`, CLI, and end-to-end compilation tests.
