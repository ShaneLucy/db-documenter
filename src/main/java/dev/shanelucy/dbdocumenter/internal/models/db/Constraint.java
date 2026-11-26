package db.documenter.internal.models.db;

import db.documenter.internal.formatter.impl.entity.ConstraintEntityLineFormatter;

/**
 * Structural column-level constraints surfaced in a PlantUML ERD.
 *
 * <p>Only constraints that convey structural relationships meaningful on an ERD are retained.
 * Physical/logical modelling constraints (CHECK, DEFAULT, AUTO_INCREMENT, NULLABLE, GENERATED) are
 * intentionally excluded — they are implementation details beyond ERD scope.
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * Column column = Column.builder()
 *     .name("email")
 *     .dataType("varchar")
 *     .isNullable(false)
 *     .constraints(List.of(Constraint.UNIQUE))
 *     .build();
 * }</pre>
 *
 * @see Column
 */
public enum Constraint {
  /** Foreign key constraint — references another table. */
  FK(0),

  /** Unique constraint — values must be unique across the table. */
  UNIQUE(1);

  private final int displayPriority;

  Constraint(final int displayPriority) {
    this.displayPriority = displayPriority;
  }

  /**
   * Returns the display priority used by {@link ConstraintEntityLineFormatter} to order constraints
   * when multiple are present on a column.
   *
   * <p>Lower values are rendered first (FK before UNIQUE).
   *
   * @return the display priority (0–1)
   */
  public int displayPriority() {
    return displayPriority;
  }
}
