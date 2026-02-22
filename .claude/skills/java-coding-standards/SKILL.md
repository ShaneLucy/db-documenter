---
name: Java Coding Standards
description: Enforces db-documenter project coding standards for non-test Java code including immutability, null safety, builder patterns, validation, Javadoc, and architectural conventions. Use this when writing or reviewing production Java code.
allowed-tools:
  - Read
  - Edit
  - Write
  - Bash
---

# Java Coding Standards for db-documenter

This skill defines the coding standards for non-test Java code in the db-documenter project. These standards ensure consistency, maintainability, and quality across the codebase.

---

## 1. Immutability and Data Modeling

### Records for Models

All domain models MUST be implemented as Java records with defensive copying:

**Rules:**
- Use records for all immutable data models
- Always defensively copy mutable collections in compact constructors using `List.copyOf()`
- Place validation in the compact constructor
- NO setters or mutable state

### Builder Pattern

All models MUST provide a builder:

**Builder Rules:**
- Builder class MUST be `static final class Builder`
- Static factory method `builder()` returns new Builder instance
- All setter methods return `this` for method chaining
- Setter method signature: `public Builder fieldName(final @NonNull Type fieldName)`
- Defensively copy collections in builder setters using `List.copyOf()`
- NO default values - all fields must be explicitly set - except for public api facing objects
- `build()` method constructs the record (validation happens in compact constructor)
- Document with Javadoc including `@return this builder instance for method chaining`

---

## 2. Code Style and Modern Java

### Final Keyword Usage

Use `final` extensively:

**Final Rules:**
- ALL method parameters must be `final`
- ALL local variables must be `final` (except when reassignment needed)
- Utility classes MUST be `final` with private constructor
- Implementation classes must be `final` unless designed for extension
- Builder classes MUST be `static final class Builder`

### Modern Java Features

Use modern Java syntax and features targeting Java 25:

**Modern Java Rules:**
- Use text blocks (`"""`) for multi-line strings (SQL preparedStatements, JSON, etc.)
- Use `var` for local variables when type is obvious from right side
- Use `List.of()`, `Set.of()`, `Map.of()` for immutable collections
- Use `List.copyOf()` for defensive copying
- Use try-with-resources for `AutoCloseable` types


### Descriptive Naming

Use clear, descriptive names:

**Naming Rules:**
- Use full words, avoid abbreviations (except widely known: SQL, DB, HTML)
- Method names must be verbs or verb phrases
- Class names must be nouns
- Boolean methods/variables must be `isXxx` or `hasXxx`
- Collection variables must be plural: `tables`, `columnNames`, `constraints`

---

## 3. Package Organization and Encapsulation

### Package Structure

Follow this package organization pattern:

**Package Rules:**
- Public API at root (`db.documenter`)
- ALL implementation details in `internal.*`
- Separate interfaces (`*.api`) from implementations (`*.impl`)
- Group by feature/responsibility, not by pattern
- Database-specific implementations in `*.impl.postgresql`, `*.impl.mysql`, etc.

### Interface vs Implementation

Separate interfaces from implementations:

**Interface Rules:**
- Define interfaces in `.api` packages
- Implementations in `.impl` packages
- Use descriptive implementation names: `PostgresqlQueryRunner`, not `QueryRunnerImpl`
- Implementations must be `final` unless designed for extension

---

## 4. Logging

### Logger Setup and Usage

Use `java.util.logging.Logger` with proper guard checks:


**Logging Rules:**
- Logger MUST be `private static final Logger LOGGER`
- Use `Logger.getLogger(ClassName.class.getName())`
- ALWAYS check `LOGGER.isLoggable(Level)` before logging
- Use parameterized messages: `LOGGER.log(Level.INFO, "Message: {0}", param)`
- Multiple params: `new Object[] {param1, param2, param3}`
- Appropriate levels:
  - `Level.SEVERE` - Errors that prevent operation
  - `Level.WARNING` - Potential problems
  - `Level.INFO` - Important business events
  - `Level.FINE` - Debug information
- Log before re-throwing exceptions
- Include context in error messages

---

## 5. Exception Handling

### Custom Exceptions

Use custom exceptions for domain-specific errors:

**Exception Rules:**
- Create custom exceptions for domain errors
- Extend `RuntimeException` for validation/programming errors
- Extend `Exception` for recoverable checked exceptions
- Include descriptive messages
- Don't catch exceptions you can't handle - let them propagate

### Exception Strategy

Use the right exception type for the situation:

- Create custom exceptions for domain-specific errors
- Extend `RuntimeException` for validation/programming errors (preferred)
- Extend `Exception` for recoverable checked exceptions (rare cases only)
- Include descriptive, actionable messages
- Always chain the cause: `throw new CustomException("message", cause)`
- Log before re-throwing: Log at error site, don't log repeatedly
- Don't catch exceptions you can't handle - let them propagate
- NEVER catch `Error`, `InterruptedException`, or `ThreadDeath`
- NEVER use empty catch blocks

## 6. Utility Classes

### Utility Class Pattern

Utility classes MUST follow this pattern:

**Utility Class Rules:**
- Constructor MUST be `private` and throw `IllegalStateException("Utility class")`
- ALL methods MUST be `static`
- Group related utility methods together

---

## 7. Enum Design

### Enum with Behavior

Enums can have fields and methods:

**Enum Rules:**
- Use enums for fixed sets of values
- Can have fields, constructors, and methods
- Fields must be `final`
- Constructor automatically private
- Provide accessor methods, not direct field access

---

## 8. Interface Design

### Clean Interface Contracts

Keep interfaces focused and clean:


**Interface Rules:**
- Single Responsibility - one clear purpose
- Document the interface contract thoroughly
- Document each method's behavior
- Declare checked exceptions
- Use domain types (Table, Column) not primitives
- Reference implementations in `@see` tags

---

## 9. SQL Queries

### SQL Query Style

Use text blocks for SQL with consistent formatting:

**SQL Query Rules:**
- Use text blocks (`"""`) for all SQL preparedStatements
- Declare as `private static final String`
- Use UPPERCASE for SQL keywords
- Use lowercase for column/table names
- Indent properly (2 spaces)
- Align JOIN conditions
- Use parameterized preparedStatements (?) for safety
- One query per constant
- Name constants descriptively: `GET_COLUMN_INFO_QUERY`

---

## 10. Resource Management

### AutoCloseable and Try-With-Resources

ALL resources implementing AutoCloseable MUST be properly closed:

**Resource Management Rules:**
- Use try-with-resources for ALL AutoCloseable types
- This includes: Connection, PreparedStatement, ResultSet, InputStream, OutputStream, Reader, Writer, etc.
- Chain resources in single try statement when possible
- NEVER rely on cascading close behavior (e.g., assuming PreparedStatement.close() closes ResultSet)
- Document resource ownership clearly in javadoc
- Resources are closed in reverse order of declaration


## 11. JDBC and Database Best Practices

### Connection Management

**Connection Rules:**
- Connections MUST be obtained from a connection manager/pool (never direct DriverManager in business logic)
- Connections MUST be closed in try-with-resources or explicit finally blocks
- Document connection lifecycle and ownership in javadoc
- NEVER store connections as instance fields (use per-operation pattern)
- Connection configuration (SSL, ports) belongs in configuration objects


### PreparedStatement Usage

**PreparedStatement Rules:**
- ALWAYS use PreparedStatements (never Statement)
- Use parameterized preparedStatements with ? placeholders (prevents SQL injection)
- NEVER concatenate user input into SQL strings
- Close PreparedStatements in try-with-resources
- Reuse PreparedStatement instances when executing same query multiple times


### ResultSet Handling

**ResultSet Rules:**
- ALWAYS close ResultSet explicitly in try-with-resources
- NEVER return ResultSet directly from methods (map to domain objects instead)
- Process ResultSet completely before closing
- Use descriptive mapper methods: `mapToTables(resultSet)`, not `map(resultSet)`
- ResultSet is only valid while connection is open


## 12. Import Organization Rules

### Import Organization
Keep imports clean and explicit:

**Import Rules:**
- Use explicit imports, NEVER wildcards (no `import foo.bar.*`)
- Organize imports in groups separated by blank lines:
    1. `java.*` and `javax.*` packages
    2. Third-party libraries
    3. Project packages (`db.documenter.*`)
- Within each group, sort alphabetically
- Remove all unused imports
- One import per line (no comma-separated imports)

## 13. Validation Strategy
Use the `Validators` utility class for all validation:

**Available Validators:**
- `Validators.isNotNull(value, "parameterName")` - Ensures value is not null
- `Validators.isNotBlank(string, "parameterName")` - Ensures string is not null, empty, or whitespace
- `Validators.containsAtLeast1Item(collection, "parameterName")` - Ensures collection has at least one element

**Validation Rules:**
- Perform ALL validation in compact constructors (for records)
- Perform ALL validation at entry points (constructor, factory methods)
- Include parameter name in validation calls for clear error messages
- Validation methods throw `ValidationException` with descriptive messages
- Validate BEFORE defensive copying
- Chain validations in logical order (null check, then content check)

## 14. Null Safety

### Compile-time null safety

Use JSpecify annotations with NullAway for compile-time null safety:

**Annotation Placement:**
- Place @NonNull AFTER final keyword: `final @NonNull String name`
- Place @NonNull BEFORE type for fields: `@NonNull String name`
- Return types: `public @NonNull List<Table> getTables()


### Run-time

Use `Validators` for run-time null safety


## Summary Checklist

When writing non-test Java code, ensure:

- ✅ Records for all immutable models
- ✅ Defensive copying with `List.copyOf()`
- ✅ `@NonNull` annotations on all reference types
- ✅ Validation in compact constructors using `Validators`
- ✅ Builder pattern with fluent API
- ✅ `final` keyword on parameters and local variables
- ✅ Comprehensive Javadoc with examples
- ✅ Package-info.java for each package
- ✅ Modern Java features (text blocks, var, getFirst(), List.of())
- ✅ Proper package organization (api/impl separation)
- ✅ Logger with `isLoggable()` guards
- ✅ Checked exceptions declared in signatures
- ✅ Utility classes are final with private constructor
- ✅ SQL preparedStatements in text blocks with parameterization
- ✅ Clean interfaces with thorough documentation
- ✅ Descriptive naming (no abbreviations)

---

## Anti-Patterns to Avoid

❌ **Mutable models** - All models must be immutable records
❌ **Direct collection exposure** - Always defensively copy with `List.copyOf()`
❌ **Missing @NonNull** - All reference types need annotations
❌ **Validation in wrong place** - Validate in compact constructors, not elsewhere
❌ **Missing Javadoc** - All public types and methods need documentation
❌ **No usage examples** - Include `<pre>{@code ... }</pre>` examples
❌ **Logging without guards** - Always check `isLoggable()` first
❌ **Non-final parameters** - Use `final` on all method parameters
❌ **String concatenation in SQL** - Use parameterized preparedStatements with `?`
❌ **Generic Exception catching** - Catch specific exception types
❌ **Missing package-info.java** - Document each package
❌ **Abbreviations in names** - Use full descriptive words
❌ **Public implementation details** - Keep internals in `internal.*` packages

---
