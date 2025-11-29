package db.documenter.internal.formatter.impl.entity;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;

/**
 * A {@link EntityLineFormatter} implementation which creates the basic structure of a line in a
 * puml file.
 */
public final class DefaultEntityLineFormatter implements EntityLineFormatter {

  /**
   * Formats a default line in a puml file. When current is null this will return a formatted string
   * containing the column name, column data type and optionally the column maximum length. When
   * current is present current will be returned.
   *
   * @param table {@link Table}
   * @param column {@link Column}
   * @param current {@link String}
   * @return the formatted line or the current line {@link String}
   */
  @Override
  public String format(final Table table, final Column column, final String current) {
    if (current != null) {
      return current;
    }

    return (column.maximumLength() > 0)
        ? String.format("%s: %s(%d)", column.name(), column.dataType(), column.maximumLength())
        : String.format("%s: %s", column.name(), column.dataType());
  }
}
