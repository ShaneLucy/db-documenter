package db.documenter.internal.formatter.impl.entity;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Table;

public final class ForeignKeyEntityLineFormatter implements EntityLineFormatter {

  @Override
  public String format(final Table table, final Column column, final String current) {

    for (final ForeignKey fk : table.foreignKeys()) {
      if (fk.sourceColumn().equalsIgnoreCase(column.name())) {
        return current + " <<FK>>";
      }
    }
    return current;
  }
}
