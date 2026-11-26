# AGENTS.md — RKCC JavaCompiler Project Context

> **MANDATORY INSTRUCTION FOR FUTURE AGENTS AND CONTRIBUTORS:**
> Read this entire `AGENTS.md` file — along with [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/TEAM_CONTRACT.md](docs/TEAM_CONTRACT.md), and [docs/PROGRESS.md](docs/PROGRESS.md) — **before** designing, editing, or implementing any milestone. Do not assume unimplemented stages or data types exist; verify the current repository state first.

## 1. Project Objective

`JavaCompiler` (`rkcc.compiler:javacompiler`) is a modular Java-to-x86 assembly compiler developed for **RKCC**.

Its core objective is to provide a clean, contract-driven compilation pipeline:

```text
Java Source Code -> Lexer -> Token Stream -> Parser -> AST -> Semantic Analyzer -> Typed AST -> Assembly Generator
```

Key goals:
- Support parallel, independent development across four team members through strict package and interface boundaries.
- Center parsing extensibility around a shared `AbstractTypeParser` architecture so new Java data types can be added incrementally without rewriting the core compiler pipeline.
- Start with an agreed **integral-language subset** and scale cleanly toward additional primitive types, `String`, arrays, and reference types.
- Maintain reproducible Maven builds and thorough JUnit 5 automated test coverage.

## 2. Team Responsibilities

| Developer | Primary Role | Owned Packages / Scope |
| --- | --- | --- |
| **Krishna** | Core Architect & Pipeline Lead | `rkcc.compiler.core`, `rkcc.compiler.lexer`, `rkcc.compiler.diagnostics`, `rkcc.compiler.integration`, shared contracts, `AbstractTypeParser` base architecture |
| **Jatin** | Frontend Syntax Lead | `rkcc.compiler.parser`, `rkcc.compiler.ast` |
| **Janesh** | Type System & Semantics Lead | `rkcc.compiler.semantic`, `rkcc.compiler.types` (initial integer type system and semantic rules; extensible type descriptors coordinated with Krishna) |
| **Riya** | Backend Codegen Lead | `rkcc.compiler.codegen` (x86 assembly generation) |

## 3. Technology Stack

- **Language:** Java (targeting `--release 17` compatibility in `pom.xml`)
- **Build Tool:** Apache Maven 3.9+ (`pom.xml` with `maven-compiler-plugin` `3.14.1` and `maven-surefire-plugin` `3.5.6`)
- **Testing Framework:** JUnit 5 (`org.junit.jupiter:junit-jupiter` `5.12.2`)
- **Version Control:** Git (remote `origin`: `https://github.com/Krishna2132/JavaCompiler.git`)
- **Proposed Backend Target:** Linux x86-64 with NASM syntax *(proposed architectural baseline only — pending formal supervisor confirmation; see [docs/PROGRESS.md](docs/PROGRESS.md))*

## 4. Module Ownership Boundaries

Every contributor (human or AI agent) must respect package ownership boundaries:

1. **Do not implement another team member's concrete module** unless explicitly authorized for an integration milestone.
2. **Contract changes require cross-module alignment:** Any modification to shared interfaces, token models, AST contracts, type descriptors, or diagnostic models must be documented in [docs/TEAM_CONTRACT.md](docs/TEAM_CONTRACT.md) and must not break downstream compilation.
3. **Acyclic dependencies:** Dependencies must flow along the compiler pipeline and shared contracts (`types`, `lexer` token contracts, `ast` node contract, `core`, `diagnostics`). `core` holds shared SPI ports (`AssemblyBackend`, `AssemblyGenerationContext`, `AbstractTypeParser`), so `types`, `lexer` token contracts, and `ast` must not depend on `core`; `codegen` implements `AssemblyBackend` without `core` depending on `codegen`. Backend (`codegen`) must never depend on `lexer` or `parser`; `parser` must never emit assembly; `AbstractTypeParser` must not duplicate `Parser` orchestration or `CodeGenerator` emission.
4. **No speculative placeholders:** Do not create empty stub classes for unimplemented stages. Only shared contracts required by active milestones (`TypeCategory`, `TypeDescriptor`, `Token`, `TokenStream`, `AstNode`, `AssemblyGenerationContext`, `AssemblyBackend`, `AbstractTypeParser`) are implemented; remaining packages stay documented via `package-info.java` until their owning milestone begins.
5. **Truthful status reporting:** Never claim that `byte`, `short`, `int`, `long`, `char`, `float`, `double`, `boolean`, `String`, arrays, reference types, or unbuilt compiler stages (`Lexer`, `Parser`, concrete AST nodes, `SemanticAnalyzer`, `CodeGenerator`, `DiagnosticReporter`, `CompilerPipeline`) are implemented until their code and passing tests exist in the repository.

## 5. Development Workflow

For every new milestone or task:

1. **Read Project Context:** Review `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/TEAM_CONTRACT.md`, and `docs/PROGRESS.md`.
2. **Verify Baseline:** Confirm the current branch (`git branch --show-current`), clean working tree (`git status`), and passing build (`mvn clean test`).
3. **Stay Within Scope:** Implement only the classes and tests assigned to the active milestone and owning developer.
4. **Write Automated Tests:** Add JUnit 5 unit tests in `src/test/java/rkcc/compiler/<module>/` covering both valid inputs and diagnostic/error cases.
5. **Validate Build:** Run `mvn clean test` after final edits and confirm zero failures, zero errors, and zero skipped tests.
6. **Update Tracking Docs:** Update `docs/PROGRESS.md` (and `README.md` or `docs/ARCHITECTURE.md` if contracts or milestones changed).
7. **Commit Cleanly:** Create meaningful, scoped commits on your feature branch. Never create empty or artificial commits.

## 6. Git Management Rules

- **Branching:** Work on your designated feature branch (e.g., `krishna/compiler-core`). Other developers work on their own branches (e.g., `jatin/parser-ast`, `janesh/semantic-types`, `riya/codegen-x86`).
- **Protected `main`:** Never commit directly to `main`, never push directly to `main`, and never force push (`git push --force`) or rewrite shared history.
- **Remote Safety:** Do not run `git push` or `git merge` unless explicitly instructed by the repository owner/user.
- **History Preservation:** Preserve existing Git history and remote configuration (`origin`).
- **Commit Messages:** Use clear Conventional Commits style (e.g., `build: ...`, `feat: ...`, `docs: ...`, `test: ...`).

## 7. Testing Requirements

- All tests must use **JUnit 5** (`org.junit.jupiter.api.*`).
- Test directory mirrors main source packages under `src/test/java/rkcc/compiler/`.
- `mvn clean test` is the mandatory verification gate before any milestone is considered complete.
- Unit tests should test module contracts in isolation using clean test fixtures or lightweight contract stubs where downstream/upstream modules are not yet implemented.
- End-to-end tests belong in `rkcc.compiler.integration` once pipeline modules are ready to integrate.
