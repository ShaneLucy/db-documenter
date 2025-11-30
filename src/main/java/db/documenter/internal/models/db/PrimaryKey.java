package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Represents a primary key constraint on a database table.
 *
 * <p>This record captures the constraint name and the column(s) that compose the primary key.
 * Supports both single-column and composite (multi-column) primary keys.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. The columnNames list is
 * defensively copied to prevent external modification.
 *
 * <p><b>Validation:</b> All parameters are validated as non-null in the compact constructor.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Single-column primary key
 * PrimaryKey pk = PrimaryKey.builder()
 *     .constraintName("pk_users")
 *     .columnNames(List.of("id"))
 *     .build();
 *
 * // Composite primary key
 * PrimaryKey compositePk = PrimaryKey.builder()
 *     .constraintName("pk_user_role")
 *     .columnNames(List.of("user_id", "role_id"))
 *     .build();
 * }</pre>
 *
 * @param constraintName the primary key constraint name
 * @param columnNames the {@link List} of column name {@link String} values forming the primary key
 *     (defensively copied)
 * @see Table
 */
public record PrimaryKey(@NonNull String constraintName, @NonNull List<String> columnNames) {

  public PrimaryKey {
    Validators.isNotNull(constraintName, "constraintName");
    Validators.isNotNull(columnNames, "columnNames");
    columnNames = List.copyOf(columnNames);
  }

  /**
   * Creates a new builder for constructing PrimaryKey instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link PrimaryKey} instances using a fluent API.
   *
   * <p><b>Design Pattern:</b> Builder pattern for flexible object construction.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * PrimaryKey pk = PrimaryKey.builder()
   *     .constraintName("pk_products")
   *     .columnNames(List.of("id"))
   *     .build();
   * }</pre>
   *
   * @see PrimaryKey
   */
  public static class Builder {
    private String constraintName;
    private List<String> columnNames;

    /**
     * Sets the primary key constraint name.
     *
     * @param constraintName the constraint name
     * @return this builder instance for method chaining
     */
    public Builder constraintName(final @NonNull String constraintName) {
      this.constraintName = constraintName;
      return this;
    }

    /**
     * Sets the column names that compose the primary key.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param columnNames the {@link List} of column name {@link String} values (defensively copied)
     * @return this builder instance for method chaining
     */
    public Builder columnNames(final @NonNull List<String> columnNames) {
      this.columnNames = List.copyOf(columnNames);
      return this;
    }

    /**
     * Builds and returns a new {@link PrimaryKey} instance.
     *
     * @return a new immutable {@link PrimaryKey} instance
     * @throws ValidationException if any required field is null
     */
    public PrimaryKey build() {
      return new PrimaryKey(constraintName, columnNames);
    }
  }
}
