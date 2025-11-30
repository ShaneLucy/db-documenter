package db.documenter.internal.models.db;

import db.documenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Represents a database column with metadata for PlantUML entity-relationship diagram generation.
 *
 * <p>This record encapsulates all column-level information including name, data type, maximum
 * length, and associated constraints (e.g., UNIQUE, CHECK, DEFAULT, AUTO_INCREMENT, NULLABLE).
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Validation:</b> All parameters are validated as non-null in the compact constructor. The
 * constraints list is defensively copied to prevent external modification.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * // Create a column with constraints using the builder
 * Column emailColumn = Column.builder()
 *     .name("email")
 *     .dataType("varchar")
 *     .maximumLength(255)
 *     .constraints(List.of(Constraint.UNIQUE, Constraint.NULLABLE))
 *     .build();
 *
 * // Create a simple column without constraints
 * Column idColumn = Column.builder()
 *     .name("id")
 *     .dataType("uuid")
 *     .build();
 * }</pre>
 *
 * @param name the column name
 * @param dataType the SQL data type (e.g., "varchar", "integer", "uuid")
 * @param maximumLength the maximum length for character types
 * @param constraints list of constraints applied to this column (defensively copied)
 * @see Constraint
 * @see Table
 */
public record Column(
    @NonNull String name,
    @NonNull String dataType,
    int maximumLength,
    @NonNull List<Constraint> constraints) {

  public Column {
    Validators.isNotNull(name, "name");
    Validators.isNotNull(dataType, "dataType");
    Validators.isNotNull(constraints, "constraints");
    constraints = List.copyOf(constraints);
  }

  /**
   * Checks if this column allows null values.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Column column = Column.builder()
   *     .name("email")
   *     .constraints(List.of(Constraint.NULLABLE))
   *     .build();
   *
   * boolean nullable = column.isNullable(); // Returns true
   * }</pre>
   *
   * @return true if the column has the NULLABLE constraint, false otherwise
   */
  public boolean isNullable() {
    return constraints.contains(Constraint.NULLABLE);
  }

  /**
   * Creates a new builder for constructing Column instances.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * Column column = Column.builder()
   *     .name("user_id")
   *     .dataType("integer")
   *     .build();
   * }</pre>
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String dataType;
    private int maximumLength;
    private List<Constraint> constraints;

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder dataType(final String dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder maximumLength(final int maximumLength) {
      this.maximumLength = maximumLength;
      return this;
    }

    public Builder constraints(@NonNull final List<Constraint> constraints) {
      this.constraints = List.copyOf(constraints);
      return this;
    }

    public Column build() {
      return new Column(name, dataType, maximumLength, constraints);
    }
  }
}
