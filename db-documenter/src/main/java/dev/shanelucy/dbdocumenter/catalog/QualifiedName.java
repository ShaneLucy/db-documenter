package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.catalog.Identifiers;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.Comparator;
import org.jspecify.annotations.Nullable;

/**
 * A schema-qualified object name, such as a table or a user-defined type.
 *
 * <p>Both parts are as stored in the catalog. They may contain whitespace or any other character,
 * so they are validated as non-empty rather than non-blank.
 *
 * <p><b>Ordering:</b> by schema, then name, in Java {@link String} order. This is consistent with
 * {@link #equals}.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param schema the containing schema; not empty
 * @param name the object name within the schema; not empty
 */
public record QualifiedName(String schema, String name) implements Comparable<QualifiedName> {

  private static final Comparator<QualifiedName> ORDER =
      Comparator.comparing(QualifiedName::schema).thenComparing(QualifiedName::name);

  /**
   * Validates both parts.
   *
   * @throws IllegalArgumentException if either part is null or empty
   */
  public QualifiedName {
    Validators.isNotEmpty(schema, "schema");
    Validators.isNotEmpty(name, "name");
  }

  /**
   * Creates a qualified name.
   *
   * @throws IllegalArgumentException if either part is null or empty
   */
  public static QualifiedName of(final String schema, final String name) {
    return new QualifiedName(schema, name);
  }

  /** Creates a new builder for {@link QualifiedName}. */
  public static Builder builder() {
    return new Builder();
  }

  // The record's generated equals and hashCode compare the same components, so they are consistent
  // with this ordering; PMD does not recognise generated record members.
  @SuppressWarnings("PMD.OverrideBothEqualsAndHashCodeOnComparable")
  @Override
  public int compareTo(final QualifiedName other) {
    return ORDER.compare(this, other);
  }

  /**
   * Returns the name as {@code schema.name} for diagnostics, such as {@code app.users} or {@code
   * main."Mixed Case"}.
   *
   * <p>Each part is bare when it matches {@code [a-z0-9_]+} and otherwise double-quoted with
   * special characters escaped, so a dot or space inside a part stays unambiguous. This format is
   * for messages and logs only and is not a contract; use {@link #schema()} and {@link #name()} to
   * read the parts.
   */
  @Override
  public String toString() {
    return Identifiers.format(this);
  }

  /** Builder for constructing {@link QualifiedName} instances using a fluent API. */
  public static final class Builder {
    private @Nullable String schema;
    private @Nullable String name;

    /** Returns this builder for method chaining. */
    public Builder schema(final String schema) {
      this.schema = schema;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * Builds the {@link QualifiedName}; both parts must have been set.
     *
     * @throws IllegalArgumentException if either part is unset or empty
     */
    public QualifiedName build() {
      return new QualifiedName(
          Validators.isNotNull(schema, "schema"), Validators.isNotNull(name, "name"));
    }
  }
}
