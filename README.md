# JavaCompiler

Modular Java-to-x86 assembly compiler infrastructure for RKCC.

## Project Introduction

`JavaCompiler` (`rkcc.compiler:javacompiler`) is a modular compiler project built in Java for RKCC. Its core design centers on an extensible abstract Java token-parsing and type-handling architecture that maps Java language constructs and data types to general x86 assembly output.

## Project Objective

- Establish a clean, modular compiler pipeline encompassing lexical analysis/tokenization, abstract type parsing, semantic handling, and x86 assembly generation.
- Provide shared interfaces and an extensible abstract base class for parsing Java tokens across multiple Java data types and emitting general x86 assembly.
- Support reliable automated unit testing and reproducible builds with Maven.

## Development Prerequisites

- **Java Development Kit (JDK):** Java 17 or newer (built and verified with JDK 21.0.10, targeting Java 17 release compatibility)
- **Build Tool:** Apache Maven 3.9+ (verified with Maven 3.9.11)
- **Version Control:** Git

## Build Instructions

From the repository root (`JavaCompiler`):

```bash
mvn clean compile
```

To package the project (including running tests):

```bash
mvn clean package
```

## Test Instructions

Run the JUnit 5 test suite using Maven Surefire:

```bash
mvn clean test
```

## Current Project Status

- **Milestone 01 — Maven Project Initialization:** Completed.
  - Standard Maven directory structure (`src/main/java/rkcc/compiler/` and `src/test/java/rkcc/compiler/`), `pom.xml` (Java 17 release target, UTF-8 encoding, JUnit 5, Maven Compiler Plugin, Maven Surefire Plugin), `.gitignore`, and a minimal build verification test are in place.
  - Compiler core architecture, abstract classes, lexer, parser, semantic analyzer, and x86 assembly generator are not yet implemented (planned for upcoming milestones).
