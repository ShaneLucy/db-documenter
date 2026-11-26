package db.documenter.internal.formatter.impl.multiplicity;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.validation.Validators;
import org.jspecify.annotations.Nullable;

/**
 * A {@link MultiplicityFormatter} implementation which adds crow's foot cardinality notation to a
 * relationship line in a puml file.
 */
public final class CardinalityFormatter implements MultiplicityFormatter {

  /**
   * Replaces the basic connector "--" with crow's foot notation based on the foreign key
   * nullability. Uses "||--o{" for nullable foreign keys (one to zero-or-many) and "||--|{" for
   * non-nullable foreign keys (one to one-or-many).
   *
   * @param foreignKey {@link ForeignKey}
   * @param currentSchemaName {@link String} the schema name of the source table (unused)
   * @param current the line built so far; must not be null because this link decorates an existing
   *     line
   * @return the current line with cardinality notation {@link String}
   */
  @Override
  public String format(
      final ForeignKey foreignKey, final String currentSchemaName, final @Nullable String current) {
    final var line = Validators.isNotNull(current, "current");
    final String cardinality = foreignKey.isNullable() ? "||--o{" : "||--|{";
    return line.replace(" -- ", " " + cardinality + " ");
  }
}
