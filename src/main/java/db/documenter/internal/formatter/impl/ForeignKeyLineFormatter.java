package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.LineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Table;

public class ForeignKeyLineFormatter implements LineFormatter {

  @Override
  public String format(Table table, Column column, String current) {
    for (ForeignKey fk : table.foreignKeys()) {
      if (fk.sourceColumn().equalsIgnoreCase(column.name())) {
        // Underline the FK column name in PUML
        String decorated = "__" + current + "__";

        // Optionally add a tooltip comment
        return decorated + String.format(" '→ %s.%s", fk.targetTable(), fk.targetColumn());
      }
    }
    return current;
  }
}
