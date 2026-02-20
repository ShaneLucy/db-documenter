package db.documenter.internal.models.db;

import db.documenter.internal.exceptions.ValidationException;
import db.documenter.internal.validation.Validators;
import java.util.List;

/**
 * Represents a database materialized view with its columns.
 *
 * <p>A materialized view is a pre-computed query result stored physically on disk. Unlike {@link
 * Table}, materialized views in PostgreSQL cannot have primary key or foreign key constraints —
 * those concepts are semantically absent, not merely empty. This is why {@code MaterializedView} is
 * a distinct record rather than a {@link Table} with empty keys.
 *
 * <p><b>Why Separate from View:</b> Although both {@link View} and {@code MaterializedView} share
 * the same structure (name and columns), they are kept as distinct types because they have
 * different PlantUML stereotypes, different database semantics (virtual vs. physically stored), and
 * may evolve independently in future implementations (e.g., materialized views can have indexes).
 *
 * <p><b>PlantUML Rendering:</b> Materialized views are rendered with the {@code
 * <<materialized_view>>} stereotype to visually distinguish them from regular views and tables. All
 * columns are rendered flat with no separator line because there is no primary key.
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
 * MaterializedView monthlySales = MaterializedView.builder()
 *     .name("monthly_sales_summary")
 *     .columns(List.of(monthColumn, totalRevenueColumn, orderCountColumn))
 *     .build();
 * }</pre>
 *
 * @param name the materialized view name as it appears in the database schema
 * @param columns the {@link List} of {@link Column} instances defining this materialized view's
 *     columns (defensively copied); never null, may be empty
 * @see View
 * @see Table
 * @see Column
 */
public record MaterializedView(String name, List<Column> columns) {

  public MaterializedView {
    Validators.isNotBlank(name, "name");
    Validators.isNotNull(columns, "columns");
    columns = List.copyOf(columns);
  }

  /**
   * Creates a new builder for constructing MaterializedView instances.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link MaterializedView} instances using a fluent API.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * MaterializedView matView = MaterializedView.builder()
   *     .name("monthly_sales_summary")
   *     .columns(List.of(monthColumn, revenueColumn))
   *     .build();
   * }</pre>
   *
   * @see MaterializedView
   */
  public static final class Builder {
    private String name;
    private List<Column> columns;

    /**
     * Sets the materialized view name.
     *
     * @param name the materialized view name
     * @return this builder instance for method chaining
     */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the columns in this materialized view.
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
     * Builds and returns a new {@link MaterializedView} instance.
     *
     * @return a new immutable {@link MaterializedView} instance
     * @throws ValidationException if any required field is null or blank
     */
    public MaterializedView build() {
      return new MaterializedView(name, columns);
    }
  }
}
