package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.LineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;
import java.util.List;

public class PrimaryKeyLineFormatter implements LineFormatter {

  @Override
  public String format(Table table, Column column, String current) {
    var pkCols = table.primaryKey() != null ? table.primaryKey().columnNames() : List.<String>of();
    if (pkCols.contains(column.name())) {
      return "**" + current + "**"; // bold PKs in PUML
    }
    return current;
  }
}
