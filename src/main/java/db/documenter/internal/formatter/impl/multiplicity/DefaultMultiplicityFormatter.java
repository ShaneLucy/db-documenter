package db.documenter.internal.formatter.impl.multiplicity;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;

/**
 * A {@link MultiplicityFormatter} implementation which creates the basic structure of a
 * relationship line in a puml file.
 */
public final class DefaultMultiplicityFormatter implements MultiplicityFormatter {

  /**
   * Formats a default relationship line in a puml file. When current is null this will return a
   * formatted string representing the basic foreign key relationship structure. When current is
   * present current will be returned. For cross-schema foreign keys, the target table is qualified
   * with the schema name.
   *
   * @param foreignKey {@link ForeignKey}
   * @param currentSchemaName {@link String} the schema name of the source table
   * @param current {@link String}
   * @return the formatted relationship line or the current line {@link String}
   */
  @Override
  public String format(
      final ForeignKey foreignKey, final String currentSchemaName, final String current) {
    if (current != null) {
      return current;
    }

    final boolean isCrossSchema = isCrossSchemaReference(foreignKey, currentSchemaName);

    final String targetTable =
        isCrossSchema
            ? qualifyTableWithSchema(foreignKey.referencedSchema(), foreignKey.targetTable())
            : foreignKey.targetTable();

    final String sourceTable =
        isCrossSchema
            ? qualifyTableWithSchema(currentSchemaName, foreignKey.sourceTable())
            : foreignKey.sourceTable();

    return String.format("%s -- %s", targetTable, sourceTable);
  }

  private boolean isCrossSchemaReference(
      final ForeignKey foreignKey, final String currentSchemaName) {
    return foreignKey.referencedSchema() != null
        && !foreignKey.referencedSchema().equals(currentSchemaName);
  }

  private String qualifyTableWithSchema(final String schema, final String table) {
    return String.format("%s.%s", schema, table);
  }
}
