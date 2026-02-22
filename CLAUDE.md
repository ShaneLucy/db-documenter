# db-documenter Project Instructions

This document provides project-specific instructions for working with the db-documenter codebase, including agent routing, skill usage, and collaboration protocols.

---

## Agent Routing Protocol

Use specialized agents for specific tasks to ensure quality and consistency:

### 1. **maven-architect** Agent

**Use when:**
- Modifying `pom.xml` or Maven build configurations
- Adding, updating, or removing dependencies
- Resolving dependency conflicts or version issues
- Configuring Maven plugins or build profiles
- Running Maven commands (`mvn clean`, `mvn install`, `mvn test`, etc.)
- Troubleshooting Maven build failures
- Optimizing build performance

**Key trait:** This agent challenges dependency choices and questions Maven configuration decisions. Trust its expertise.

**Cross-domain collaboration:**
When tasks overlap with other domains, this agent delegates to specialized agents:
- **DevOps/Docker impacts**: Use `devops-lead` when dependency changes affect Docker builds or CI/CD pipelines
- **Java code modifications**: Use `java-code-writer` for any Java source code changes needed
- **Test updates**: Use `java-test-automation-specialist` when test dependencies or test execution changes
- **Code review**: Use `tech-lead-reviewer` for architectural review of dependency choices

The maven-architect remains the orchestrator for build configuration but respects domain expertise. Example: "This dependency addition requires updating the Docker build. Let me engage the devops-lead agent to update the Dockerfile accordingly."

### 2. **java-code-writer** Agent

**Use when:**
- Writing new Java classes, interfaces, enums, or records
- Implementing service classes, repositories, or utilities
- Creating domain models or DTOs
- Writing any production Java code

**Must be used with:** `java-coding-standards` and `javadoc-standards` skills (automatically available)

**After code generation:** must use `java-test-automation-specialist` to create comprehensive tests.

**Cross-domain collaboration:**
When tasks overlap with other domains, this agent delegates to specialized agents:
- **Maven dependencies**: Use `maven-architect` when new dependencies are needed for the implementation
- **Test creation**: Use `java-test-automation-specialist` to create comprehensive tests (ALWAYS after writing code)
- **Code review**: Use `tech-lead-reviewer` to review the generated code for quality and design issues
- **DevOps considerations**: Use `devops-lead` if the new code affects deployment or containerization

The java-code-writer focuses on production code while delegating testing, dependency management, and review to specialists. Example: "I've implemented the DatabaseConnectionPool class. Now let me use the java-test-automation-specialist to create comprehensive tests for it."

### 3. **java-test-automation-specialist** Agent

**Use when:**
- Creating unit tests for Java code
- Creating integration tests
- Improving existing test coverage
- Reviewing test quality

**Must be used with:** `java-test-coding-standards` skill (automatically available)

**Proactive usage:** After writing application code, automatically engage this agent to create tests.

**Cross-domain collaboration:**
When tasks overlap with other domains, this agent delegates to specialized agents:
- **Test dependencies**: Use `maven-architect` when test dependencies (JUnit, Mockito, TestContainers, etc.) need to be added or updated
- **Code under test issues**: Use `tech-lead-reviewer` when the code under test has testability issues that require refactoring
- **Java code modifications**: Use `java-code-writer` if test utilities or test fixtures need to be created as production code
- **DevOps/CI integration**: Use `devops-lead` when test execution needs to be integrated into CI/CD pipelines

The java-test-automation-specialist focuses on test creation while delegating build configuration and code design concerns. Example: "These tests require Testcontainers. Let me use the maven-architect to add the appropriate test dependencies."

### 4. **tech-lead-reviewer** Agent

**CRITICAL: Use this agent frequently for quality assurance**

**Use when:**
- **Before planning** - To collaboratively explore implementation approaches
- **During planning** - To validate architectural decisions and identify edge cases
- **After planning** - To review implementation plans before coding begins
- **Code review** - To review completed code for quality, security, and maintainability issues
- **Architectural decisions** - When choosing between design patterns or approaches
- **Before refactoring** - To ensure the approach is sound
- **User proposes an approach** - To validate or challenge the proposed solution

**Key trait:** This agent is designed to challenge assumptions and provide objective feedback. **Its feedback should be taken seriously and used to challenge the user's opinions when appropriate.**

**Cross-domain collaboration:**
When reviewing code or plans, this agent identifies domain-specific concerns and delegates implementation:
- **Maven/build issues**: Use `maven-architect` for dependency conflicts, plugin configuration, or build optimization concerns
- **Implementation work**: Use `java-code-writer` when code needs to be written or refactored based on review feedback
- **Test coverage gaps**: Use `java-test-automation-specialist` when tests are missing or inadequate
- **DevOps/deployment concerns**: Use `devops-lead` for containerization, CI/CD, or deployment-related issues

The tech-lead-reviewer focuses on architectural and design review while delegating implementation tasks. Example: "The review identified that the resource management could cause connection leaks. Let me engage the java-code-writer to implement proper try-with-resources handling."

### 5. **devops-lead** Agent

**Use when:**
- Creating or modifying Dockerfiles
- Configuring GitHub Actions workflows or CI/CD pipelines
- Planning deployment strategies
- Optimizing container images
- Troubleshooting build or deployment issues
- Making architectural decisions about deployment patterns
- Setting up container orchestration

**Key trait:** This agent challenges DevOps approaches and questions deployment decisions with a focus on security, performance, and maintainability. Trust its expertise.

**Cross-domain collaboration:**
When tasks overlap with other domains, this agent delegates to specialized agents:
- **Maven configuration**: Use `maven-architect` for pom.xml changes, dependency updates, or Maven plugin configuration
- **Java code changes**: Use `java-code-writer` for any application code modifications
- **Test execution**: Use `java-test-automation-specialist` for test creation or CI/CD test integration
- **Architecture review**: Use `tech-lead-reviewer` to validate deployment architecture decisions

The devops-lead remains the orchestrator for DevOps tasks but respects domain expertise. Example: "For this Docker optimization, we need to update the Maven build. Let me engage the maven-architect to handle the pom.xml changes, then I'll integrate those into our multi-stage Docker build."

---

## Skill Usage Protocol

### Skills Overview

1. **java-coding-standards** - Production Java coding standards (immutability, null safety, builders, validation, Javadoc, package organization, logging, resource management, JDBC best practices)

2. **javadoc-standards** - Javadoc documentation standards (focuses on "why" not "what", concise model docs, proper `@param`/`@return`/`@throws` usage)

3. **java-test-coding-standards** - Test code standards (test naming, assertions, parameterized tests, testcontainers, nested tests)

### Automatic Skill Invocation

**When writing production Java code:**
- Automatically invoke `java-coding-standards` skill
- Automatically invoke `javadoc-standards` skill

**When writing test Java code:**
- Automatically invoke `java-test-coding-standards` skill

**When reviewing code:**
- Invoke relevant skills based on code type

### How to Invoke Skills

Use the Skill tool to invoke skills:
```
Skill: "java-coding-standards"
Skill: "javadoc-standards"
Skill: "java-test-coding-standards"
```

Skills must be invoked at the start of code writing tasks to ensure standards are loaded and followed.

---

## Challenging User Opinions Protocol

**IMPORTANT:** You should constructively challenge the user's opinions and proposed approaches when:

1. **Agent feedback contradicts user's approach**
   - When `tech-lead-reviewer` identifies issues or suggests alternatives
   - When `maven-architect` questions dependency choices
   - When any agent raises security, performance, or architectural concerns

2. **Standards violations detected**
   - When user's proposed approach violates coding standards
   - When better alternatives exist that align with project standards

3. **How to challenge:**
   - Present agent feedback objectively
   - Explain the concerns and reasoning
   - Ask probing questions: "The tech-lead-reviewer raised concerns about X. Have you considered Y?"
   - Suggest alternatives with tradeoffs
   - **Do NOT simply agree with the user if agent feedback indicates a better approach**
   - Frame as collaboration: "Let's explore this together based on the feedback..."

4. **Example scenario:**
   ```
   User: "I want to use inheritance to extend PostgresqlQueryRunner for MySQL support"

   You: "Before proceeding, let me engage the tech-lead-reviewer to evaluate this approach."

   [tech-lead-reviewer responds with concerns about tight coupling, suggests composition]

   You: "The tech-lead-reviewer has raised important concerns about inheritance
   creating tight coupling between PostgreSQL and MySQL implementations. They
   suggest composition with a shared interface might be more maintainable.

   The key concern is that PostgreSQL and MySQL have different information_schema
   structures, and inheritance might make it harder to handle MySQL-specific quirks.

   What are your thoughts on the composition approach? Would you like to explore
   that alternative?"
   ```

---

## Code Quality Standards Summary

### Production Java Code Must Have:
-  Immutable records with defensive copying
-  Builders for all models (`static final class Builder`)
-  `@NonNull` annotations on all reference types
-  Validation in compact constructors using `Validators` utility
-  `final` on all parameters and local variables
-  Comprehensive Javadoc (class-level and public methods)
-  Proper package organization (`internal.*` for implementation details)
-  Logger with `isLoggable()` guards
-  Try-with-resources for all `AutoCloseable` types
-  Parameterized SQL preparedStatements (never string concatenation)
-  Modern Java features (text blocks, `var`, `List.of()`, `.getFirst()`)

### Test Code Must Have:
-  Test class name: `{ClassUnderTest}Test`
-  Descriptive test names starting with `it` or `when`
-  `@Nested` classes for grouping related tests
-  `final var` for local variables
-  Builders for creating test objects
-  `@ParameterizedTest` for multiple scenarios
-  Exception type AND message verification
-  Testcontainers for integration tests

---

## Workflow for Common Tasks

### Adding New Production Code
1. Invoke `java-coding-standards` and `javadoc-standards` skills
2. Use `java-code-writer` agent to implement code
3. Consider `tech-lead-reviewer` agent for code review
4. Use `java-test-automation-specialist` agent to create tests

### Planning New Features
1. **FIRST:** Engage `tech-lead-reviewer` to collaboratively explore approaches
2. Discuss alternatives and tradeoffs with the user
3. Once approach is agreed, use appropriate agents for implementation
4. Review completed code with `tech-lead-reviewer` before considering it done

### Modifying Build Configuration
1. Use `maven-architect` agent for ALL pom.xml changes
2. Trust its expertise when it questions your choices
3. Run verification commands it suggests

### Reviewing Existing Code
1. Use `tech-lead-reviewer` agent with reference to appropriate skills
2. Present findings to user with prioritized recommendations
3. Challenge any proposed fixes that violate standards

### Refactoring
1. **BEFORE starting:** Engage `tech-lead-reviewer` to validate approach
2. Ensure tests exist (use `java-test-automation-specialist` if needed)
3. Implement refactoring following coding standards
4. Run tests and builds to verify

---

## Maven Command Permissions

You have permission to run the following Maven commands without user approval:
- `mvn spotless:apply` (code formatting)
- `mvn test` (run tests)
- `mvn clean` (clean build)

For other Maven operations, use the `maven-architect` agent.

---

## Key Project Characteristics

- **Target Java Version:** Java 25 for both application code and test
- **Build Tool:** Maven
- **Logging:** `java.util.logging.Logger` (not SLF4J)
- **Null Safety:** JSpecify annotations with NullAway
- **Code Style:** Enforced by SpotBugs, Checkstyle, and Spotless
- **Immutability:** All models are immutable records
- **Database Support:** Currently PostgreSQL (designed for multi-database extension)

---

## Critical Reminders

1. **Always challenge assumptions** - Use agents to validate approaches before implementing
2. **Trust agent expertise** - Especially `tech-lead-reviewer` and `maven-architect`
3. **Standards are mandatory** - All code must follow the defined skills
4. **Test coverage matters** - Use `java-test-automation-specialist` proactively
5. **Collaborative, not prescriptive** - Present agent feedback and discuss alternatives with the user
6. **Question user's approach when agents identify issues** - Don't blindly agree if better alternatives exist
