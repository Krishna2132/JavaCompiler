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
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — Compiler pipeline diagram, package architecture, stage specifications, `AbstractTypeParser` contract design, and type extensibility roadmap.
- [docs/TEAM_CONTRACT.md](docs/TEAM_CONTRACT.md) — Team responsibilities, shared interface boundaries, integration rules, and testing expectations.
- [docs/PROGRESS.md](docs/PROGRESS.md) — Completed/pending milestones, verified test results, known limitations, and unresolved technical decisions.

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
- **Milestone 08 — Type Registry & Type Parser Management:** Completed.
  - Implemented `TypeRegistry` (`rkcc.compiler.core`) managing `AbstractTypeParser` registrations, atomic multi-type registration, case-sensitive lookups, token-based resolution (primitive keywords + `String` identifier), duplicate/conflict rejection, and `DiagnosticCollector` integration.
  - Extended `AbstractTypeParser` with `getSupportedTypeNames()` and added category classification helpers on `TypeCategory`.
  - Verified with `mvn clean test`: 56 tests passing (`BuildVerificationTest`: 1, `AbstractTypeParserTest`: 9, `TypeRegistryTest`: 6, `DiagnosticIntegrationTest`: 6, `DiagnosticsTest`: 5, `SourceLocationTest`: 3, `LexerTest`: 9, `ListTokenStreamTest`: 9, `TokenTest`: 5, `TokenTypeTest`: 3).
- **Not Yet Implemented:** `Parser`, concrete AST nodes, `SemanticAnalyzer`, production Java type handlers (`byte`, `short`, `int`, `long`, `char`, `float`, `double`, `boolean`, `String`, arrays, reference types — demonstrated only via test-only parsers in `TypeRegistryTest`), `AssemblyBackend` / `CodeGenerator` backend, and `CompilerPipeline`.
