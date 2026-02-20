package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;

/**
 * Represents a database view with its columns.
 *
 * <p>A view is a virtual table defined by a query. Unlike {@link Table}, views in PostgreSQL cannot
 * have primary key or foreign key constraints — those concepts are semantically absent, not merely
 * empty. This is why {@code View} is a distinct record rather than a {@link Table} with an empty
 * primary key.
 *
 * <p><b>PlantUML Rendering:</b> Views are rendered with the {@code <<view>>} stereotype to visually
 * distinguish them from tables, materialized views, and other schema objects. Because views have no
 * primary key, all columns are rendered flat with no separator line.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe. The columns list is defensively
 * copied to prevent external modification.
 *
 * <p><b>Validation:</b> All parameters are validated in the compact constructor. The name must not
 * be blank and columns must not be null.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * View activeUsersView = View.builder()
 *     .name("active_users_view")
 *     .columns(List.of(idColumn, nameColumn, emailColumn))
 *     .build();
 * }</pre>
 *
 * @param name the view name as it appears in the database schema
 * @param columns the {@link List} of {@link Column} instances defining this view's columns
 *     (defensively copied); never null, may be empty for views with no selectable columns
 * @see MaterializedView
 * @see Table
 * @see Column
 */
public record View(String name, List<Column> columns) {

  public View {
    Validators.isNotBlank(name, "name");
    Validators.isNotNull(columns, "columns");
    columns = List.copyOf(columns);
  }

  /**
   * Creates a new builder for constructing View instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link View} instances using a fluent API.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * View view = View.builder()
   *     .name("active_users_view")
   *     .columns(List.of(idColumn, nameColumn))
   *     .build();
   * }</pre>
   *
   * @see View
   */
  public static final class Builder {
    private String name;
    private List<Column> columns;

    /**
     * Sets the view name.
     *
     * @param name the view name
     * @return this builder instance for method chaining
     */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the columns in this view.
     *
     * <p><b>Defensive Copying:</b> The provided list is defensively copied to ensure immutability.
     *
     * @param columns the {@link List} of {@link Column} instances (defensively copied)
     * @return this builder instance for method chaining
     */
    public Builder columns(final List<Column> columns) {
      this.columns = List.copyOf(columns);
      return this;
    }

    /**
     * Builds and returns a new {@link View} instance.
     *
     * @return a new immutable {@link View} instance
     * @throws ValidationException if any required field is null or blank
     */
    public View build() {
      return new View(name, columns);
    }
  }
}
