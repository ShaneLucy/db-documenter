package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.LineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;

public class DefaultLineFormatter implements LineFormatter {

  @Override
  public String format(final Table table, final Column column, final String current) {
    if (current != null) return current;
    return (column.maximumLength() > 0)
        ? String.format("%s: %s(%d)", column.name(), column.dataType(), column.maximumLength())
        : String.format("%s: %s", column.name(), column.dataType());
  }
}
