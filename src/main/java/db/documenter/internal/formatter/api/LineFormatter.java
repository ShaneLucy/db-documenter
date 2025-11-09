package db.documenter.internal.formatter.api;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;

@FunctionalInterface
public interface LineFormatter {

  String format(final Table table, final Column column, final String current);
}
