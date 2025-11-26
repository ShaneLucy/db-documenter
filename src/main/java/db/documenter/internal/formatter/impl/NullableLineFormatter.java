package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.LineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;

public final class NullableLineFormatter implements LineFormatter {

  @Override
  public String format(final Table table, final Column column, final String current) {
    return column.isNullable() ? current + "?" : current;
  }
}
