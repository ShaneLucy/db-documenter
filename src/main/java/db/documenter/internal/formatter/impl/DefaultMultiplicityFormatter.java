package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;

/**
 * A {@link MultiplicityFormatter} implementation which creates the basic structure of a
 * relationship line in a puml file.
 */
public final class DefaultMultiplicityFormatter implements MultiplicityFormatter {

  /**
   * Formats a default relationship line in a puml file. When current is null this will return a
   * formatted string representing the basic foreign key relationship structure. When current is
   * present current will be returned.
   *
   * @param foreignKey {@link ForeignKey}
   * @param current {@link String}
   * @return the formatted relationship line or the current line {@link String}
   */
  @Override
  public String format(final ForeignKey foreignKey, final String current) {
    if (current != null) {
      return current;
    }

    return String.format("%s -- %s", foreignKey.targetTable(), foreignKey.sourceTable());
  }
}
