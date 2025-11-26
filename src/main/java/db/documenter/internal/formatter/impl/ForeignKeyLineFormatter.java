package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.LineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Table;

public final class ForeignKeyLineFormatter implements LineFormatter {

  @Override
  public String format(final Table table, final Column column, final String current) {

    for (final ForeignKey fk : table.foreignKeys()) {
      if (fk.sourceColumn().equalsIgnoreCase(column.name())) {

        final String decorated = "__" + current + "__";

        // Optionally add a tooltip comment
        return decorated + String.format(" '→ %s.%s", fk.targetTable(), fk.targetColumn());
      }
    }
    return current;
  }
}
