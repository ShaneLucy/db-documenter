package db.documenter.internal.formatter.impl.entity;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.Table;
import java.util.stream.Collectors;

public final class ConstraintEntityLineFormatter implements EntityLineFormatter {

  @Override
  public String format(final Table table, final Column column, final String current) {
    if (column.constraints().isEmpty()) {
      return current;
    }

    final var constraintString =
        column.constraints().stream()
            .sorted((c1, c2) -> Integer.compare(c1.displayPriority(), c2.displayPriority()))
            .map(constraint -> formatConstraint(constraint, column))
            .collect(Collectors.joining(","));

    return current + " <<" + constraintString + ">>";
  }

  /**
   * Formats a single constraint for display, qualifying composite UNIQUE constraints with their
   * constraint name.
   *
   * <p>A composite UNIQUE constraint covers multiple columns, so distinguishing it by name allows
   * diagram readers to correlate which columns share the same UNIQUE constraint without inspecting
   * the DDL directly. Single-column UNIQUE constraints render as the plain {@code UNIQUE} label
   * since no disambiguation is needed.
   *
   * @param constraint the constraint to format
   * @param column the column whose metadata supplies the composite constraint name
   * @return {@code "UNIQUE:constraint_name"} for composite UNIQUE, or the enum name otherwise
   */
  private String formatConstraint(final Constraint constraint, final Column column) {
    if (constraint == Constraint.UNIQUE && column.compositeUniqueConstraintName() != null) {
      return "UNIQUE:" + column.compositeUniqueConstraintName();
    }
    return constraint.name();
  }
}
