package db.documenter.internal.formatter.api;

import db.documenter.internal.models.db.ForeignKey;
import org.jspecify.annotations.Nullable;

/**
 * Contract for one link in a chain that builds a single relationship line of the ERD.
 *
 * <p>The {@code current} argument is the line accumulated by earlier links in the chain; it is
 * {@code null} for the first link, which is responsible for creating the initial line.
 */
@FunctionalInterface
public interface MultiplicityFormatter {

  String format(
      final ForeignKey foreignKey, final String currentSchemaName, final @Nullable String current);
}
