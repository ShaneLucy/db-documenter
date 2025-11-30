package db.documenter.internal.models.db;

/**
 * Enum representing column-level constraints in a database table.
 *
 * <p>Each constraint type has an associated display priority used for consistent ordering when
 * multiple constraints are present on a single column.
 *
 * <p><b>Constraint Types:</b>
 *
 * <ul>
 *   <li>{@link #FK} - Foreign key constraint
 *   <li>{@link #UNIQUE} - Unique constraint
 *   <li>{@link #AUTO_INCREMENT} - Auto-incrementing column (e.g., serial, sequence)
 *   <li>{@link #DEFAULT} - Has a default value
 *   <li>{@link #CHECK} - Check constraint (validation rule)
 *   <li>{@link #NULLABLE} - Allows NULL values
 * </ul>
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * Column column = Column.builder()
 *     .name("email")
 *     .dataType("varchar")
 *     .constraints(List.of(Constraint.UNIQUE, Constraint.NULLABLE))
 *     .build();
 * }</pre>
 *
 * @see Column
 */
public enum Constraint {
  /** Foreign key constraint - references another table. */
  FK(0),

  /** Unique constraint - values must be unique across the table. */
  UNIQUE(1),

  /** Auto-increment constraint - column value is automatically generated (e.g., serial). */
  AUTO_INCREMENT(2),

  /** Default value constraint - column has a default value if not specified. */
  DEFAULT(3),

  /** Check constraint - column must satisfy a validation rule (e.g., age &gt; 0). */
  CHECK(4),

  /** Nullable constraint - column allows NULL values. */
  NULLABLE(5);

  private final int displayPriority;

  Constraint(final int displayPriority) {
    this.displayPriority = displayPriority;
  }

  /**
   * Returns the display priority for this constraint.
   *
   * <p>Lower values are displayed first when formatting multiple constraints on a column. This
   * ensures consistent ordering (e.g., FK before UNIQUE before NULLABLE).
   *
   * @return the display priority (0-5)
   */
  public int displayPriority() {
    return displayPriority;
  }
}
