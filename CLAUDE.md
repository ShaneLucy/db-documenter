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
- Prefer Java Streams API over enhanced for loops for collection processing

### Logging Patterns

Use `java.util.logging.Logger` for all logging in this project:

**Logger Declaration:**
```java
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MyClass {
  private static final Logger LOGGER = Logger.getLogger(MyClass.class.getName());

  // ... rest of class
}
```

**Important Logging Rules:**

1. **Use Parameterized Logging**: Always use parameterized logging with `LOGGER.log()` instead of string concatenation
   ```java
   // Good: Uses parameterized logging
   if (LOGGER.isLoggable(Level.INFO)) {
     LOGGER.log(Level.INFO, "Building schema: {0}", schemaName);
   }

   // Good: Multiple parameters
   if (LOGGER.isLoggable(Level.INFO)) {
     LOGGER.log(
         Level.INFO,
         "Completed schema: {0} ({1} tables, {2} enums)",
         new Object[] {schemaName, tables.size(), dbEnums.size()});
   }

   // Bad: String concatenation
   if (LOGGER.isLoggable(Level.INFO)) {
     LOGGER.info("Building schema: " + schemaName);
   }
   ```

2. **Check Log Level Before Logging**: Always check if the log level is enabled before constructing expensive log messages or calling LOGGER.log()
   ```java
   // Good: Only logs if INFO level is enabled
   if (LOGGER.isLoggable(Level.INFO)) {
     LOGGER.log(Level.INFO, "Discovered: {0} tables in schema: {1}", new Object[] {count, schema});
   }

   // Bad: Always constructs the log call even if logging is disabled
   LOGGER.log(Level.INFO, "Discovered: {0} tables in schema: {1}", new Object[] {count, schema});
   ```

3. **Log Before Rethrowing**: When catching and rethrowing exceptions, log the error with context before rethrowing
   ```java
   try {
     // ... operation
   } catch (SQLException e) {
     if (LOGGER.isLoggable(Level.SEVERE)) {
       LOGGER.log(
           Level.SEVERE,
           "Failed to build schema: {0} - {1}",
           new Object[] {schemaName, e.getMessage()});
     }
     throw e;  // Rethrow the original exception
   }
   ```

4. **Use Appropriate Log Levels**:
   - `Level.SEVERE`: Error conditions that prevent normal operation
   - `Level.WARNING`: Potential problems that don't prevent operation
   - `Level.INFO`: Informational messages about normal operations (connections, discovery counts, processing steps)
   - `Level.FINE/FINER/FINEST`: Debug-level details

5. **Include Context**: Log messages should include relevant context (e.g., schema name, table name, counts) to aid debugging using parameterized logging

**When to Log:**
- Before rethrowing exceptions (with context) - use SEVERE level
- When establishing database connections - use INFO level
- When discovering database objects (tables, enums, columns, etc.) - use INFO level
- At key processing milestones (building schemas, tables, rendering output) - use INFO level
- Error conditions that require investigation - use SEVERE level

**When NOT to Log:**
- Don't log-and-wrap exceptions in RuntimeException - prefer rethrowing the original exception with logging
- Don't log in pure transformation functions (mappers) unless there's a specific reason - logging belongs in orchestration layers (builders)
- Avoid excessive logging in tight loops - consider logging summaries instead

### Functional Programming with Streams

Prefer using Java Streams API over enhanced for loops when processing collections:

**Prefer (Stream API):**
```java
return items.stream()
    .map(item -> transform(item))
    .filter(item -> item.isValid())
    .toList();
```

**Avoid (Enhanced for loop):**
```java
final List<Result> result = new ArrayList<>();
for (final Item item : items) {
    final var transformed = transform(item);
    if (transformed.isValid()) {
        result.add(transformed);
    }
}
return result;
```

**Exception Handling in Streams:**
When working with checked exceptions inside stream operations, wrap them in `RuntimeException`:
```java
return items.stream()
    .map(item -> {
        try {
            return processWithCheckedException(item);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    })
    .toList();
```

**When to Use For Loops:**
- Simple iteration where streams would reduce readability
- Performance-critical tight loops where stream overhead matters
- Complex multi-step operations that don't map well to stream operations

### Defensive Copying

To prevent SpotBugs EI_EXPOSE_REP and EI_EXPOSE_REP2 violations, use defensive copying for mutable collection fields:

**In Record Compact Constructors:**
```java
public record Column(String name, List<Constraint> constraints) {
  public Column {
    // Defensive copy: prevents external modification
    constraints = constraints == null ? List.of() : List.copyOf(constraints);
  }
}
```

**In Builder Methods:**
```java
public Builder constraints(final List<Constraint> constraints) {
  // Defensive copy in builder
  this.constraints = constraints == null ? List.of() : List.copyOf(constraints);
  return this;
}
```

**When to Use Defensive Copying:**
- Records/classes that expose `List`, `Set`, `Map`, or other mutable collections
- Builder pattern methods that accept mutable collections
- Any public method that stores a reference to a mutable object

**When NOT to Use Defensive Copying:**
- Immutable types (`String`, `Integer`, primitives, enums)
- Internal private methods where mutation is controlled
- Performance-critical paths where immutability is guaranteed by design

### Dependency Injection

All internal components use constructor-based dependency injection for better testability and flexibility:

**Prefer (Constructor Injection):**
```java
public final class TableBuilder {
  private final ColumnMapper columnMapper;
  private final ForeignKeyMapper foreignKeyMapper;

  public TableBuilder(
      final ColumnMapper columnMapper,
      final ForeignKeyMapper foreignKeyMapper) {
    this.columnMapper = columnMapper;
    this.foreignKeyMapper = foreignKeyMapper;
  }
}
```

**Avoid (Field Instantiation):**
```java
public final class TableBuilder {
  private final ColumnMapper columnMapper = new ColumnMapper();
  private final ForeignKeyMapper foreignKeyMapper = new ForeignKeyMapper();
}
```

**Benefits of Constructor Injection:**
- Testability: Can inject mocks for unit testing
- Explicit dependencies: Clear what a class needs to function
- Immutability: Dependencies are final and set at construction
- Flexibility: Can swap implementations without changing the class

**Exception - Public Entry Points (Facade Pattern):**
For public-facing classes like `DbDocumenter`, use the facade pattern to hide internal complexity:
- Constructor takes only essential configuration (`DbDocumenterConfig`)
- Wire up all internal dependencies inside the constructor
- Keeps public API simple while maintaining clean internal architecture

```java
public final class DbDocumenter {
  private final FormatterConfigurer formatterConfigurer;
  private final SchemaBuilder schemaBuilder;

  public DbDocumenter(final DbDocumenterConfig dbDocumenterConfig) {
    // Simple public API - only takes config
    this.formatterConfigurer = new FormatterConfigurer();

    // Wire up internal dependencies with DI
    final ColumnMapper columnMapper = new ColumnMapper();
    final TableBuilder tableBuilder = new TableBuilder(columnMapper, ...);
    this.schemaBuilder = new SchemaBuilder(..., tableBuilder);
  }
}
```

## Architecture

The codebase follows a layered architecture with clear separation of concerns:

### Core Flow

1. **Entry Point** (`DbDocumenter.java`): Facade that orchestrates the entire generation process
2. **Configuration** (`DbDocumenterConfig`): Database connection and schema configuration with validation
3. **Connection Layer** (`internal/connection`): Database-specific connection management
4. **Query Layer** (`internal/queries`): Executes database-specific queries to extract metadata
5. **Mapper Layer** (`internal/mapper`): Transforms raw query results into enriched domain objects
6. **Builder Layer** (`internal/builder`): Orchestrates data fetching and transformation
7. **Model Layer** (`internal/models/db`): Domain objects (Schema, Table, Column, ForeignKey, PrimaryKey, DbEnum)
8. **Formatter Layer** (`internal/formatter`): Transforms column metadata into PlantUML syntax
9. **Renderer Layer** (`internal/renderer`): Assembles formatters and models into final PlantUML output

**Data Flow:**
```
Database → QueryRunner → Mapper → Builder → Model → Formatter → Renderer → PlantUML
```

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

### Builder and Mapper Layers

The application separates data orchestration (builders) from data transformation (mappers):

**Builder Layer** (`internal/builder`):
- `FormatterConfigurer`: Creates and configures formatter instances
- `SchemaBuilder`: Orchestrates schema building, manages database connections
- `TableBuilder`: Builds tables by coordinating queries and mappers
- `EnumBuilder`: Builds database enum types
- Responsibilities: Query orchestration, connection lifecycle, high-level workflow

**Mapper Layer** (`internal/mapper`):
- `ColumnMapper`: Maps USER-DEFINED types to enum types
- `ForeignKeyMapper`: Enriches foreign keys with nullability information
- `TableMapper`: Combines table components into table instances
- Responsibilities: Pure data transformation, no database access, stateless operations

**Design Principles:**
- Builders orchestrate and manage resources (connections, transactions)
- Mappers are pure functions that transform data
- Both use dependency injection for testability
- Clear separation of concerns between fetching and transforming

**Example:**
```java
// Builder orchestrates
public List<Table> buildTables(QueryRunner queryRunner, String schema, List<DbEnum> dbEnums) {
  return tableNames.stream()
      .map(tableName -> {
          final List<Column> rawColumns = queryRunner.getColumnInfo(schema, tableName);
          final List<Column> columns = columnMapper.mapUserDefinedTypes(rawColumns, dbEnums);
          // ... combine with mapper
      })
      .toList();
}

// Mapper transforms
public List<Column> mapUserDefinedTypes(List<Column> rawColumns, List<DbEnum> dbEnums) {
  return rawColumns.stream()
      .map(column -> /* pure transformation */)
      .toList();
}
```

### Formatter Chain Pattern

The application uses two separate formatter chains for different purposes:

**Entity Column Formatting** uses `EntityLineFormatter` via `CompositeEntityLineFormatter`:

```java
CompositeEntityLineFormatter.builder()
    .addFormatter(new DefaultEntityLineFormatter())        // Base: name + dataType
    .addFormatter(new PrimaryKeyEntityLineFormatter())     // Adds PK indicator
    .addFormatter(new ForeignKeyEntityLineFormatter())     // Adds FK indicator
    .addFormatter(new NullableEntityLineFormatter())       // Adds nullability
    .addFormatter(new ConstraintEntityLineFormatter())     // Adds constraints (UNIQUE, CHECK, DEFAULT, AUTO_INCREMENT)
    .build();
```

Each `EntityLineFormatter` implementation:
- Receives the current formatted string
- Appends additional notation
- Returns the enhanced string
- Follows functional interface pattern: `format(Table, Column, String) -> String`
- Example output: `id: uuid <<DEFAULT,AUTO_INCREMENT>>`

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

**IMPORTANT Testing Requirements:**

1. **Test-Driven Development**: When writing new functionality, write tests at the same time (not after)
   - Write the test first or alongside the implementation
   - Ensures code is designed for testability from the start
   - Prevents "we'll add tests later" technical debt

2. **Test New Code and Edits**: ALWAYS create or update tests when writing or modifying code
   - **New code**: Create corresponding test files with comprehensive test coverage
   - **Modified code**: Update existing tests to cover new behavior and edge cases
   - **New methods**: Add test cases for all new methods, including edge cases
   - **Bug fixes**: Add regression tests that verify the bug is fixed
   - This is MANDATORY - never consider code complete without tests

3. **Maintain Tests**: When changing existing functionality, check and update related tests
   - Run affected tests after making changes
   - Update test expectations to match new behavior
   - Add new test cases for new edge cases or scenarios
   - Never comment out or delete failing tests without fixing them

4. **Use JUnit Assertions**: This project uses JUnit 5 assertions, NOT AssertJ
   - Use `assertEquals()`, `assertTrue()`, `assertFalse()`, `assertNotNull()`, etc.
   - Use `assertThrows()` for exception testing
   - **DO NOT** use AssertJ's `assertThat()` (not a dependency in this project)

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
   - Good: `whenCurrentIsNullReturnsFormattedRelationship`, `buildsSchemaWithTablesAndEnums`
   - Bad: `testFormat`, `test1`, `test`

4. **Use JUnit 5 assertions** (import from `org.junit.jupiter.api.Assertions`):
   ```java
   import static org.junit.jupiter.api.Assertions.*;

   // Equality checks
   assertEquals(expected, actual);
   assertEquals("expected message", result.getMessage());

   // Boolean checks
   assertTrue(result.isEmpty());
   assertFalse(result.isPresent());

   // Null checks
   assertNotNull(result);
   assertNull(result);

   // Exception testing
   final SQLException exception = assertThrows(
       SQLException.class,
       () -> builder.buildEnums(queryRunner, "schema")
   );
   assertEquals("Connection failed", exception.getMessage());

   // Instance checks
   assertTrue(exception.getCause() instanceof SQLException);
   ```

5. **Mock dependencies with Mockito**:
   ```java
   @ExtendWith(MockitoExtension.class)
   class MyTest {
       @Mock private QueryRunner queryRunner;

       @BeforeEach
       void setUp() {
           reset(queryRunner);  // Reset mocks before each test
       }

       @Test
       void testSomething() {
           when(queryRunner.getTableInfo("schema")).thenReturn(List.of());
           verify(queryRunner).getTableInfo("schema");
       }
   }
   ```

6. **Follow existing patterns**: Look at similar test files for guidance
   - Entity formatter tests: See `DefaultLineFormatterTest`, `PrimaryKeyLineFormatterTest`
   - Multiplicity formatter tests: See `DefaultMultiplicityFormatterTest`, `CardinalityFormatterTest`
   - Composite formatter tests: See `CompositeEntityLineFormatterTest`, `CompositeMultiplicityFormatterTest`
   - Builder tests: See `EnumBuilderTest`, `TableBuilderTest`, `SchemaBuilderTest`

### Test Coverage Requirements

- Test the happy path (normal operation)
- Test edge cases (nulls, empty strings, whitespace)
- Test decorator pattern (when `current` is null vs not null)
- For composite formatters: test empty list, single formatter, multiple formatters
- Use Mockito for testing composite formatters (`@Mock`, `@ExtendWith(MockitoExtension.class)`)

### Workflow After Writing Tests

Always format code and run full verification:

```bash
mvn spotless:apply && mvn clean verify
```

This ensures:
1. Code is formatted according to Google Java Style
2. Tests pass
3. All static analysis checks pass (Checkstyle, SpotBugs, PMD)
4. No issues will occur during the full build

For faster iteration during development, you can use `mvn spotless:apply && mvn test`, but always run the full `mvn clean verify` before committing.

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

## Column Constraints

The application detects and renders database constraints in PlantUML using inline `<<>>` notation:

**Supported Constraints:**
- `UNIQUE`: Columns with unique constraints
- `CHECK`: Columns with validation rules (e.g., `age > 0`)
- `DEFAULT`: Columns with default values
- `AUTO_INCREMENT`: Serial/auto-incrementing columns (PostgreSQL: detects `nextval` sequences)

**Architecture:**
1. `PostgresqlQueryRunner.GET_COLUMN_INFO_QUERY` fetches constraint metadata from `information_schema`
2. `PostgresqlResultSetMapper.buildConstraints()` maps raw SQL results to `List<Constraint>` enum
3. `Column` record stores constraints as an immutable list (defensive copy in compact constructor)
4. `ConstraintEntityLineFormatter` formats constraints as comma-separated values: `<<UNIQUE,CHECK,DEFAULT>>`

**Example Output:**
```
id: uuid <<DEFAULT,AUTO_INCREMENT>>
email: varchar <<UNIQUE>>
age: integer <<CHECK>>
```

## Foreign Key Multiplicity

Foreign keys track nullability to determine relationship cardinality:

- `ForeignKey.combineForeignKeyAndIsNullable()` merges FK metadata with column nullability
- Used by `CardinalityFormatter` to add crow's foot notation
- Format: `target_table ||--o{ source_table` (nullable) or `target_table ||--|{ source_table` (non-nullable)
- Target table (referenced table) appears on the left, source table (FK holder) on the right
- Enables accurate one-to-many, one-to-one, etc. relationship rendering
