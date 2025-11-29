package db.documenter.internal.formatter.impl.entity;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;

public final class NullableEntityLineFormatter implements EntityLineFormatter {

  @Override
  public String format(final Table table, final Column column, final String current) {
    return column.isNullable() ? current + "?" : current;
  }
}
