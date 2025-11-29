package db.documenter.internal.formatter.api;

import db.documenter.internal.models.db.ForeignKey;

@FunctionalInterface
public interface MultiplicityFormatter {

  String format(final ForeignKey foreignKey, final String current);
}
