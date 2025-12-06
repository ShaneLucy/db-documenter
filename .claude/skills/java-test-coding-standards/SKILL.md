---
name: Java Test Coding Standards
description: Enforces db-documenter project test coding standards for Java test code targeting Java 25. Use this when writing or reviewing test code.
allowed-tools:
  - Read
  - Edit
  - Write
  - Bash
---

# Java Test Coding Standards for db-documenter

This skill defines the coding standards for Java test code in the db-documenter project, targeting Java 25.

---

## 1. Test Class Structure

### Naming and Organization

**Test Class Rules:**
- Test class name MUST be `{ClassUnderTest}Test` (e.g., `TableMapper` → `TableMapperTest`)
- Test classes MUST mirror production package structure under `src/test/java`
- Use `@Nested` inner classes to group related tests with descriptive names ending in `Tests`
- Nested class names describe the method or scenario being tested (e.g., `FormatTests`, `CombineTableComponentsTests`)

### Fields and Setup

**Field and Setup Rules:**
- Test fixture fields are NOT final (they're reassigned in setup methods)
- Use `@BeforeEach` for per-test setup
- Use `@BeforeAll` / `@AfterAll` for expensive shared setup (database containers, connections)
- Static fields for shared resources MUST be `private static final`
- Setup method signature: `void setUp()` or `static void containerSetUp()`

---

## 2. Test Method Naming

### Descriptive Test Names

Use clear, behavior-focused test method names:

**Test Naming Rules:**
- Start with `it` for behavior: `itDoesNotThrowException`, `itThrowsExceptionWhenValueIsNull`
- Start with `when` for conditional scenarios: `whenNoConstraintsReturnsCurrentAsIs`
- Use complete sentences in camelCase describing the expected behavior
- Be specific about the scenario and expected outcome
- NO underscores in test method names

**Examples:**
```java
@Test
void itCombinesTableComponentsIntoTable() { ... }

@Test
void itThrowsExceptionWhenListIsEmpty() { ... }

@Test
void whenSingleConstraintAppendsInBrackets() { ... }
```

---

## 3. Assertions and Modern Java

### Assertion Style

**Assertion Rules:**
- Static import ALL assertions: `import static org.junit.jupiter.api.Assertions.*`
- Use `assertEquals(expected, actual)` for equality checks
- Use `assertTrue()` / `assertFalse()` for boolean conditions
- Use `assertNotNull()` for null checks
- Use `assertThrows()` for exception verification
- Use `assertDoesNotThrow()` to verify no exception is thrown
- Capture exception for message verification: `final var exception = assertThrows(...)`

### Java 25 Features in Tests

**Modern Java Rules:**
- Use `final var` for local variables in tests
- Use `final` keyword on method parameters (same as production code)
- Use `List.of()`, `Set.of()`, `Map.of()` for immutable test data
- Use `.getFirst()` instead of `.get(0)` for first element
- Use text blocks (`"""`) for multi-line string comparisons
- Use `String.formatted()` for string formatting

---

## 4. Test Data and Builders

### Test Data Creation

**Test Data Rules:**
- Use builder pattern for creating test objects (same as production code)
- Inline test data in the test method (keep data close to test)
- Use realistic, meaningful test data
- Use `List.of()` for simple immutable collections
- Create reusable builders in `@BeforeEach` when shared across tests

**Example:**
```java
@BeforeEach
void setUp() {
  columnBuilder = Column.builder().name("col").dataType("varchar");
}

@Test
void whenSingleConstraintAppendsInBrackets() {
  final var column = columnBuilder.constraints(List.of(Constraint.UNIQUE)).build();
  final var result = formatter.format(table, column, "value");
  assertEquals("value <<UNIQUE>>", result);
}
```

---

## 5. Parameterized Tests

### Using @ParameterizedTest

**Parameterized Test Rules:**
- Use `@ParameterizedTest` for testing multiple inputs with same logic
- Use `@ValueSource` for simple primitive/string arrays
- Use `@MethodSource` for complex object arrays
- Method parameter names MUST be `final` (same as production code)
- Test name should indicate it handles multiple cases

**Example:**
```java
@ParameterizedTest
@ValueSource(strings = {"test", "test test", " test", " test "})
void itDoesNotThrowExceptionWhenStringIsValid(final String input) {
  assertDoesNotThrow(() -> Validators.isNotBlank(input, "test"));
}
```

---

## 6. Integration Tests and Test Containers

### Database Testing with Testcontainers

**Integration Test Rules:**
- Use `@BeforeAll` to start containers (expensive operation)
- Use `@AfterAll` to stop containers and close connections
- Share containers across tests in the same nested class
- Store container reference as `private static final`
- Close connections in try-catch or `@AfterAll`
- Initialize database state in `@BeforeAll`
- Extract test environment setup to helper classes (e.g., `PostgresTestEnvironment`)

**Example:**
```java
@Nested
class DatabaseIntegrationTests {
  private static final PostgresTestEnvironment POSTGRES_TEST_ENVIRONMENT =
      new PostgresTestEnvironment();
  private static Connection connection;

  @BeforeAll
  static void containerSetUp() throws SQLException, IOException {
    POSTGRES_TEST_ENVIRONMENT.startContainer();
    connection = POSTGRES_TEST_ENVIRONMENT.getConnection();
    POSTGRES_TEST_ENVIRONMENT.initialiseDatabase(connection, "/test-db.sql");
  }

  @AfterAll
  static void containerClearDown() throws SQLException {
    connection.close();
    POSTGRES_TEST_ENVIRONMENT.stop();
  }
}
```

---

## 7. Import Organization

### Test Import Rules

**Import Rules (same as production):**
- Use explicit imports, NEVER wildcards
- Organize imports in groups:
  1. Static imports (`import static org.junit.jupiter.api.Assertions.*`)
  2. `java.*` and `javax.*` packages
  3. Third-party libraries (JUnit, Mockito, etc.)
  4. Project packages (`db.documenter.*`)
- Within each group, sort alphabetically
- Remove all unused imports

---

## 8. Testing Exceptions

### Exception Testing Pattern

**Exception Testing Rules:**
- Use `assertThrows()` to verify exceptions are thrown
- Capture exception to verify message: `final var exception = assertThrows(...)`
- Verify exception message with `assertEquals(expectedMessage, exception.getMessage())`
- Test both the exception type AND the message
- Use `assertDoesNotThrow()` to verify success cases

**Example:**
```java
@Test
void itThrowsExceptionWhenStringIsBlank() {
  final var exception =
      assertThrows(ValidationException.class, () -> Validators.isNotBlank("", "test"));

  assertEquals("test must not be blank", exception.getMessage());
}

@Test
void itDoesNotThrowExceptionWhenStringIsValid() {
  assertDoesNotThrow(() -> Validators.isNotBlank("valid", "test"));
}
```

---

## 9. Test Organization Patterns

### Nested Test Classes

**Nested Test Rules:**
- Use `@Nested` to group tests for a specific method or scenario
- Nested class name describes what's being tested + "Tests" suffix
- Each nested class can have its own `@BeforeEach` setup
- Share setup in outer class when appropriate
- Keep tests focused and isolated

**Example:**
```java
class ValidatorsTest {

  @Nested
  class IsNotBlankTests {
    @Test
    void itDoesNotThrowExceptionWhenStringIsValid() { ... }

    @Test
    void itThrowsExceptionWhenStringIsNull() { ... }
  }

  @Nested
  class IsNotNullTests {
    @Test
    void itDoesNotThrowExceptionWhenValueIsNotNull() { ... }

    @Test
    void itThrowsExceptionWhenValueIsNull() { ... }
  }
}
```

---

## 10. Key Differences from Production Code

### Test-Specific Relaxations

**Rules that differ from production code:**
- ✅ Test fixture fields are NOT final (reassigned in `@BeforeEach`)
- ✅ Local variables still use `final var`
- ✅ Method parameters still use `final` keyword
- ✅ NO Javadoc required for test methods (test name is the documentation)
- ✅ NO null safety annotations required (`@NonNull` / `@Nullable`)
- ✅ Mutable state is acceptable in test classes for fixtures

---

## Summary Checklist

When writing Java test code, ensure:

- ✅ Test class name is `{ClassUnderTest}Test`
- ✅ Static import JUnit assertions
- ✅ Use `@Nested` classes to group related tests
- ✅ Test method names start with `it` or `when` and describe behavior
- ✅ Use `final var` for local variables
- ✅ Use `final` on method parameters
- ✅ Use `List.of()`, `.getFirst()`, and Java 25 features
- ✅ Use builders for creating test objects
- ✅ Use `@ParameterizedTest` for multiple input scenarios
- ✅ Use `@BeforeAll` / `@AfterAll` for expensive setup
- ✅ Test exception type AND message
- ✅ Clean imports (no wildcards)

---

## Anti-Patterns to Avoid

❌ **Final test fields** - Test fixtures should not be final (reassigned in setup)
❌ **Wildcard imports** - Always use explicit imports
❌ **Generic test names** - Use descriptive names starting with `it` or `when`
❌ **Missing exception message checks** - Always verify the exception message
❌ **Container per test** - Share expensive resources with `@BeforeAll`
❌ **Test data in separate files** - Keep data close to test (inline)
❌ **Not using builders** - Use builder pattern for complex objects
❌ **Underscores in test names** - Use camelCase only

---
