---
name: Javadoc Standards
description: Enforces javadoc documentation standards for the db-documenter project. Focuses on concise, purposeful documentation that explains "why" rather than restating the obvious. Use when writing or reviewing javadoc comments.
allowed-tools:
  - Read
  - Edit
  - Write
  - Bash
---

# Javadoc Standards for db-documenter

This skill defines javadoc documentation standards for the db-documenter project. These standards emphasize concise, meaningful documentation that focuses on the "why" and avoids redundancy.


## Core Principles

### 1. Document the "Why", Not the "What"

Focus on **purpose, constraints, and non-obvious behavior** rather than restating what the code already says.

### 2. Be Concise, Especially for Models

Model classes should have lean documentation:
- Document the class purpose and key characteristics
- Avoid repeating information that's obvious from the code
- Focus on validation rules, immutability, and usage constraints

### 3. Don't Restate the Obvious

Skip documentation when the code is self-explanatory:
- Simple getters/accessors
- Obvious parameter names
- Standard builder patterns


## When to Document (and When to Skip)

### ✅ Always Document

- **Public API classes and interfaces** - All public types must have class-level javadoc
- **Public methods with non-obvious behavior** - Explain what isn't clear from the signature
- **Constraints and validation rules** - Document what's validated and when
- **Thread safety characteristics** - Especially for immutable/thread-safe types
- **Package purposes** - Every package needs `package-info.java`
- **Complex algorithms or business logic** - Explain the approach and rationale
- **Exceptions and error conditions** - When they occur and why
- **Format specifications** - Date formats, patterns, expected input formats

### ⚠️ Document Selectively

- **Builder setter methods** - Document the builder class itself; individual setters only if they have special behavior
- **Simple utility methods** - Only if there are gotchas or non-obvious constraints
- **Parameter details** - Only document constraints, formats, or non-obvious aspects

### ❌ Skip Documentation

- **Obvious record accessors** - If a method just returns a field with no logic
- **Standard builder boilerplate** - Don't document every setter if they all follow the same pattern
- **Package-private implementation methods** - Focus on public API
- **Self-explanatory parameters** - If the parameter name and type are clear

---

## Class-Level Documentation

### Record/Model Classes

Model classes should be concise with a clear structure:

**Template:**
```java
/**
 * [One-sentence description of what this represents]
 *
 * <p>[Additional context about purpose and usage - 1-2 sentences]
 *
 * <p><b>Immutability:</b> [Thread-safety and immutability guarantees]
 *
 * <p><b>Validation:</b> [What's validated and when]
 *
 * @param field1 [constraints, format, or purpose - not just restating the name]
 * @param field2 [constraints, format, or purpose]
 * @see RelatedClass1
 * @see RelatedClass2
 */
```

### Interface Classes

Interfaces should document the contract thoroughly.


### Implementation Classes

Focus on what makes this implementation special.

### Utility Classes

Document the purpose and scope.

---

## Method Documentation

### Public Methods

Focus on **behavior, constraints, and side effects**:

### Builder Methods

**Even more minimal (if all setters follow same pattern):**
```java
public static class Builder {

  /** @return this builder for method chaining */
  public Builder name(final @NonNull String name) { }

  /** @return this builder for method chaining */
  public Builder tables(final @NonNull List<Table> tables) { }
}
```

---

## Parameter Documentation (`@param`)

### Focus on Constraints, Purpose, and Format

Don't just restate the parameter name - add value:

### What to Include in `@param`

Document these aspects when relevant:
- **Constraints**: "must not be empty", "must be positive", "must be unique"
- **Format expectations**: "ISO-8601 format", "comma-separated values", "supports % wildcards"
- **Validation rules**: "validated as non-blank", "must contain at least one element"
- **Defensive copying**: "defensively copied to ensure immutability"
- **Default behavior**: "if null, defaults to empty list"
- **Purpose/usage**: "used to filter results", "determines connection timeout"

### When to Skip `@param`

Skip when the parameter is completely obvious:
```java
// Skip javadoc - completely obvious
public Builder name(final @NonNull String name) {
  this.name = name;
  return this;
}
```

---

## Return Documentation (`@return`)

### Explain What's Returned and Important Characteristics

**Good:**
```java
/** @return immutable list of tables in this schema; never null, may be empty */
```

### What to Include in `@return`

- **Nullability**: "never null", "null if not found", "empty list if none exist"
- **Immutability**: "immutable list", "defensive copy", "read-only view"
- **State**: "new instance", "singleton instance", "cached result"
- **Conditions**: "true if column is nullable", "empty optional if not configured"

### Standard Builder Return

For builder methods, use a consistent short form:
```java
/** @return this builder for method chaining */
```

---

## Exception Documentation (`@throws`)

### Document Circumstances, Not Restatements

Explain **when** and **why** the exception occurs, not just restating validation.

**Bad:**
```java
/**
 * @throws ValidationException if name is null
 * @throws ValidationException if tables is null
 * @throws ValidationException if dbEnums is null
 */
```

**Good (Option 1 - Document at constructor/method level):**
```java
/**
 * @throws ValidationException if validation fails - typically due to null, blank,
 *         or empty required fields
 */
public Schema { }
```

**Good (Option 2 - For specific conditions):**
```java
/**
 * @throws SQLException if database connection fails or query execution errors occur
 * @throws ValidationException if schema name is not found in the database
 */
public List<Table> getTables(String schemaName) throws SQLException { }
```

### When to Document Exceptions

- **Checked exceptions** - Always document (required by compiler)
- **Unchecked exceptions with specific meaning** - Document when they indicate specific error conditions
- **Generic validation exceptions** - Document once at class level, not per-parameter
- **Runtime exceptions from bugs** - Don't document IllegalStateException or programming errors

---

## Using `{@link}` and `@see`

### `{@link}` - Inline References

Use `{@link}` for inline references within flowing text:

```java
/**
 * Represents a {@link Schema} containing multiple {@link Table} instances.
 * Each table has {@link Column} definitions and optional {@link ForeignKey} relationships.
 */
```

**When to use:**
- Within descriptions and explanations
- When the reference is part of the sentence flow
- For types mentioned in the narrative

### `@see` - Related References

Use `@see` for related classes/methods listed at the end:

```java
/**
 * Represents a database column...
 *
 * @param name column identifier
 * @param dataType SQL data type
 * @see Constraint
 * @see Table
 * @see ForeignKey
 */
```

**When to use:**
- For "relates to" relationships
- Related classes that provide context
- Alternative implementations
- Parent/child relationships

### Combining Both

```java
/**
 * Maps {@link ResultSet} rows to {@link Column} instances.
 *
 * <p>Extracts column metadata including name, type, constraints, and relationships.
 * Delegates foreign key mapping to {@link ForeignKeyMapper}.
 *
 * @see Table
 * @see ColumnMapper
 */
```

---

## Code Examples

### When to Include Examples

**Include examples for:**
- Complex builder patterns (especially models)
- Non-obvious API usage
- Multiple ways to achieve the same goal
- Common use cases that aren't immediately clear

**Skip examples for:**
- Trivial getters/setters
- Obvious single-method calls
- Standard builder patterns everyone knows

### Example Format

Use `<pre>{@code ...}</pre>` for code blocks:

```java
/**
 * Creates a new builder for constructing Schema instances.
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * Schema schema = Schema.builder()
 *     .name("public")
 *     .tables(List.of(usersTable, ordersTable))
 *     .dbEnums(List.of(statusEnum))
 *     .build();
 * }</pre>
 *
 * @return new builder instance
 */
public static Builder builder() { }
```

### Keep Examples Minimal

Show just enough to understand usage:

**Concise and clear:**
```java
Schema schema = Schema.builder()
    .name("public")
    .tables(List.of(table1, table2))
    .dbEnums(List.of(enum1))
    .build();
```

---

## Special Documentation Sections

### Thread Safety

For immutable records and thread-safe classes:

```java
/**
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 * All collections are defensively copied.
 */
```

For non-thread-safe classes (rare):

```java
/**
 * <p><b>Thread Safety:</b> This class is NOT thread-safe. External
 * synchronization required for concurrent access.
 */
```

### Validation

Document validation rules clearly:

```java
/**
 * <p><b>Validation:</b> All parameters are validated in the compact constructor.
 * The name must not be blank, and collections must not be null. Defensive
 * copies prevent external modification.
 */
```

### Resource Management

For classes dealing with AutoCloseable resources:

```java
/**
 * <p><b>Resource Management:</b> Connections returned by this manager must be
 * closed using try-with-resources. Failure to close connections will cause
 * connection pool exhaustion.
 */
```

### Defensive Copying

When defensive copying is significant:

```java
/**
 * @param tables list of tables in this schema (defensively copied)
 */
```

Or at class level:

```java
/**
 * <p><b>Defensive Copying:</b> All collections are defensively copied using
 * {@link List#copyOf(Collection)} to ensure immutability.
 */
```

---

## HTML Formatting in Javadoc

### Use Sparingly but Effectively

**Common HTML tags:**
- `<p>` - Separate paragraphs (blank lines don't work in javadoc)
- `<b>` - Emphasize section headers like "Validation:", "Thread Safety:"
- `<ul>` and `<li>` - Bulleted lists
- `<pre>{@code ...}</pre>` - Code examples
- `{@code ...}` - Inline code references

**Example:**
```java
/**
 * Validates database connection parameters.
 *
 * <p>Checks the following constraints:
 * <ul>
 *   <li>Host must not be blank</li>
 *   <li>Port must be between 1 and 65535</li>
 *   <li>Database name must not be blank</li>
 * </ul>
 *
 * <p><b>Note:</b> SSL configuration is optional and defaults to disabled.
 *
 * @throws ValidationException if any constraint is violated
 */
```

### Don't Overuse

Prefer simple text when possible. Only use HTML when it adds clarity.

---

## Summary Sentence (First Sentence)

### First Sentence is Special

The first sentence appears in javadoc summaries and indexes. Make it count.

**Rules:**
- Complete, standalone sentence
- Describes what the class/method **is** or **does**
- Avoid starting with "This method..." or "This class..."
- Use active voice

**Bad:**
```java
/** This method creates a new Schema instance from the builder. */
```

**Good:**
```java
/** Creates a new Schema instance from the builder. */
```

**Better:**
```java
/** Builds and returns a new immutable {@link Schema} instance. */
```

---

## Package Documentation

### `package-info.java` for Every Package

Every package needs a `package-info.java` file documenting its purpose:

**Template:**
```java
/**
 * [One-sentence description of package purpose]
 *
 * <p>[Additional context about what's in this package - 1-2 sentences]
 *
 * <p>[Optional: Key characteristics or patterns used]
 *
 * @see ImportantClass1
 * @see ImportantClass2
 */
package your.package.name;
```

---

## Versioning and Deprecation

### `@since` Tag

Use for versioned APIs:

```java
/**
 * Renders PlantUML diagrams with custom themes.
 *
 * @since 2.0
 */
public class ThemedRenderer { }
```

### `@deprecated` Tag

Always include migration guidance:

```java
/**
 * Connects to database using legacy configuration.
 *
 * @deprecated Use {@link #connect(DatabaseConfig)} instead. This method
 *             will be removed in version 3.0.
 */
@Deprecated
public Connection connect(String url, String user, String password) { }
```

---

## Consistency and Boilerplate

### Standardize Common Phrases

Use consistent wording for common patterns:

**Immutability:**
- "This record is immutable and thread-safe"
- "Returns an immutable list"
- "Defensively copied to ensure immutability"

**Validation:**
- "Validated in the compact constructor"
- "Validation failures throw {@link ValidationException}"
- "All fields are validated as non-null"

**Collections:**
- "never null, may be empty"
- "defensively copied"
- "immutable list"

**Builder pattern:**
- "Builder for constructing {@link ClassName} instances using a fluent API"
- "this builder for method chaining"
- "All fields must be set before calling {@link #build()}"

**Resource management:**
- "must be closed using try-with-resources"
- "caller is responsible for closing"

---

## Anti-Patterns to Avoid

❌ **Restating the obvious** - Don't document what's already clear from the code
❌ **Repeating parameter names** - `@param name the name` adds no value
❌ **Excessive builder docs** - Don't document every setter if they're all identical
❌ **Implementation details in public API** - Focus on the contract, not internals
❌ **"This method..." phrasing** - Use active voice: "Creates..." not "This method creates..."
❌ **Documenting every exception individually** - Group validation exceptions at class level
❌ **Missing summary sentence** - First sentence must be standalone and descriptive
❌ **Overusing examples** - Only include examples when they add clarity
❌ **Inconsistent phrasing** - Use standard phrases for common patterns
❌ **Missing package-info.java** - Every package needs documentation
❌ **Redundant thread-safety docs** - Document once, not on every method

---

## Quick Reference Checklist

When writing javadoc, ensure:

- ✅ Class-level javadoc on all public types
- ✅ First sentence is complete and descriptive
- ✅ Focus on "why" and constraints, not "what"
- ✅ Document validation rules (at class level for models)
- ✅ Document thread safety for concurrent classes
- ✅ Use `{@link}` for inline references
- ✅ Use `@see` for related types
- ✅ Include examples for complex APIs
- ✅ Document exception circumstances, not just types
- ✅ `@param` describes constraints and format
- ✅ `@return` describes characteristics (nullability, immutability)
- ✅ Use `<b>` for section headers like "Validation:", "Thread Safety:"
- ✅ Package-info.java exists for each package
- ✅ Consistent phrasing for common patterns
- ✅ Skip obvious getters and simple builders
- ✅ Keep it concise - especially for models

---
