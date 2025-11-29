package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;

/**
 * A {@link MultiplicityFormatter} implementation which adds crow's foot cardinality notation to a
 * relationship line in a puml file.
 */
public final class CardinalityFormatter implements MultiplicityFormatter {

  /**
   * Replaces the basic connector "--" with crow's foot notation based on the foreign key
   * nullability. Uses "||--o{" for nullable foreign keys (one to zero-or-many) and "||--|{" for
   * non-nullable foreign keys (one to one-or-many).
   *
   * @param foreignKey {@link ForeignKey}
   * @param current {@link String}
   * @return the current line with cardinality notation {@link String}
   */
  @Override
  public String format(final ForeignKey foreignKey, final String current) {
    final String cardinality = foreignKey.isNullable() ? "||--o{" : "||--|{";
    return current.replace(" -- ", " " + cardinality + " ");
  }
}
