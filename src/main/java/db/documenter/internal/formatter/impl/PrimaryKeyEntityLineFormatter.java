package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;
import java.util.List;

public final class PrimaryKeyEntityLineFormatter implements EntityLineFormatter {

  @Override
  public String format(final Table table, final Column column, final String current) {
    final var primaryKeys =
        table.primaryKey() != null ? table.primaryKey().columnNames() : List.<String>of();
    if (primaryKeys.contains(column.name())) {
      return "**" + current + "**";
    }
    return current;
  }
}
