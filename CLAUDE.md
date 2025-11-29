# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

db-documenter is a Java 21 application that generates PlantUML Entity-Relationship diagrams from database schemas. It currently supports PostgreSQL and produces `.puml` files representing database tables, columns, relationships, and custom enum types.

## Build & Test Commands

```bash
# Clean build with all verification
mvn clean verify

# Run tests only
mvn test

# Run a single test class
mvn test -Dtest=DbDocumenterTest

# Run a specific test method
mvn test -Dtest=DbDocumenterTest#testMethodName

# Build JAR (skipping tests)
mvn clean package -DskipTests

# Format code (Google Java Format via Spotless)
mvn spotless:apply

# Run static analysis only
mvn checkstyle:check spotbugs:check pmd:check spotless:check
```

**IMPORTANT**: After making any code changes, always run `mvn spotless:apply` to format the code before running tests. The recommended workflow is:

```bash
# After writing/editing code
mvn spotless:apply && mvn test
```

This ensures code is properly formatted and prevents spotless check failures during `mvn verify`.

## Code Quality Standards

This project enforces strict code quality standards:

- **Checkstyle**: Google Java Style Guide with extensions (currently set to `severity="warning"` in checkstyle.xml:22)
- **Spotless**: Google Java Format (auto-formatting)
- **SpotBugs**: Bug pattern detection
- **PMD**: Java best practices rules
- **JaCoCo**: Code coverage reporting (target/reports/jacoco)

All checks run during `mvn verify`. Code must pass all checks before committing.

### Important Style Rules

- 2-space indentation
- 100-character line limit
- All parameters, local variables, and fields must be `final`
- Public methods require Javadoc
- No star imports

## Architecture

The codebase follows a layered architecture with clear separation of concerns:

### Core Flow

1. **Entry Point** (`DbDocumenter.java`): Orchestrates the entire generation process
2. **Configuration** (`DbDocumenterConfig`): Database connection and schema configuration with validation
3. **Connection Layer** (`internal/connection`): Database-specific connection management
4. **Query Layer** (`internal/queries`): Executes database-specific queries to extract metadata
5. **Model Layer** (`internal/models/db`): Domain objects (Schema, Table, Column, ForeignKey, PrimaryKey, DbEnum)
6. **Formatter Layer** (`internal/formatter`): Transforms column metadata into PlantUML syntax
7. **Renderer Layer** (`internal/renderer`): Assembles formatters and models into final PlantUML output

### Database Abstraction

The application uses a factory pattern for database support:

- `ConnectionManagerFactory`: Creates database-specific connection managers
- `QueryRunnerFactory`: Creates database-specific query runners
- Currently implemented: PostgreSQL (`internal/connection/impl/postgresql`, `internal/queries/impl/postgresql`)

To add support for a new database:
1. Implement `ConnectionManager` interface
2. Implement `QueryRunner` interface
3. Add new `RdbmsTypes` enum value
4. Update both factory classes

### Formatter Chain Pattern

The application uses two separate formatter chains for different purposes:

**Entity Column Formatting** uses `EntityLineFormatter` via `CompositeEntityLineFormatter`:

```java
CompositeEntityLineFormatter.builder()
    .addFormatter(new DefaultLineFormatter())        // Base: name + dataType
    .addFormatter(new PrimaryKeyLineFormatter())     // Adds PK indicator
    .addFormatter(new ForeignKeyLineFormatter())     // Adds FK indicator
    .addFormatter(new NullableLineFormatter())       // Adds nullability
    .build();
```

Each `EntityLineFormatter` implementation:
- Receives the current formatted string
- Appends additional notation
- Returns the enhanced string
- Follows functional interface pattern: `format(Table, Column, String) -> String`

**Relationship Formatting** uses `MultiplicityFormatter` via `CompositeMultiplicityFormatter`:

```java
CompositeMultiplicityFormatter.builder()
    .addFormatter(new DefaultMultiplicityFormatter())  // Base: target -- source
    .addFormatter(new CardinalityFormatter())          // Adds crow's foot notation
    .build();
```

Each `MultiplicityFormatter` implementation:
- Receives the current formatted string
- Appends or transforms relationship notation
- Returns the enhanced string
- Follows functional interface pattern: `format(ForeignKey, String) -> String`
- Uses crow's foot notation: `||--o{` (nullable/zero-or-many) or `||--|{` (non-nullable/one-or-many)

### PlantUML Rendering

The rendering layer uses `PumlRenderer<T>` interface:

- `SchemaRenderer`: Top-level renderer, outputs @startuml/@enduml wrapper and packages
- `EntityRenderer`: Renders individual tables/entities using `EntityLineFormatter`
- `EnumRenderer`: Renders database enum types
- `RelationshipRenderer`: Generates relationship lines between entities using `MultiplicityFormatter`

### Testing Strategy

Tests use Testcontainers for database integration tests:

- `DatabaseTestEnvironment`: Abstract base for database test containers
- `PostgresTestEnvironment`: PostgreSQL-specific test setup with SQL initialization
- Integration tests validate the entire flow from database connection through PlantUML generation
- Formatter tests are isolated unit tests with mocked dependencies

## Writing Tests

All tests follow consistent patterns. When writing new tests:

### Test Class Structure

1. **Use `@BeforeEach` for setup**: Create new instances of the class under test in a setup method
   ```java
   private MyFormatter myFormatter;

   @BeforeEach
   void setUp() {
       myFormatter = new MyFormatter();
   }
   ```

2. **Use `@Nested` classes**: Group related tests logically
   ```java
   @Nested
   class FormatTests {
       @Test
       void testCase1() { ... }
   }
   ```

3. **Descriptive test names**: Use full sentences that describe the behavior
   - Good: `whenCurrentIsNullReturnsFormattedRelationship`
   - Bad: `testFormat`, `test1`

4. **Follow existing patterns**: Look at similar test files for guidance
   - Entity formatter tests: See `DefaultLineFormatterTest`, `PrimaryKeyLineFormatterTest`
   - Multiplicity formatter tests: See `DefaultMultiplicityFormatterTest`, `CardinalityFormatterTest`
   - Composite formatter tests: See `CompositeEntityLineFormatterTest`, `CompositeMultiplicityFormatterTest`

### Test Coverage Requirements

- Test the happy path (normal operation)
- Test edge cases (nulls, empty strings, whitespace)
- Test decorator pattern (when `current` is null vs not null)
- For composite formatters: test empty list, single formatter, multiple formatters
- Use Mockito for testing composite formatters (`@Mock`, `@ExtendWith(MockitoExtension.class)`)

### Workflow After Writing Tests

Always format code and run tests together:

```bash
mvn spotless:apply && mvn test
```

This ensures:
1. Code is formatted according to Google Java Style
2. Tests pass
3. No spotless check failures will occur during `mvn verify`

### Builder Pattern in Tests

Use builder pattern for creating test data:
```java
final var fk = ForeignKey.builder()
    .sourceTable("orders")
    .targetTable("users")
    .isNullable(true)
    .build();
```

## Working with Enums

The application handles database-specific enum types (e.g., PostgreSQL `CREATE TYPE` enums):

1. `QueryRunner.getEnumInfo()` fetches enum type definitions
2. `QueryRunner.getEnumValues()` fetches allowed values for each enum
3. Columns with `dataType="USER-DEFINED"` are mapped to their enum types via `Column.mapUserDefinedToEnumType()`
4. `EnumRenderer` outputs PlantUML enum notation

## Foreign Key Multiplicity

Foreign keys track nullability to determine relationship cardinality:

- `ForeignKey.combineForeignKeyAndIsNullable()` merges FK metadata with column nullability
- Used by `CardinalityFormatter` to add crow's foot notation
- Format: `target_table ||--o{ source_table` (nullable) or `target_table ||--|{ source_table` (non-nullable)
- Target table (referenced table) appears on the left, source table (FK holder) on the right
- Enables accurate one-to-many, one-to-one, etc. relationship rendering
