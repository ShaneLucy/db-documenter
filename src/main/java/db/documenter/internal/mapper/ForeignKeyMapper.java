package db.documenter.internal.mapper;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import java.util.List;

/** Maps raw foreign key metadata to enriched foreign key instances. */
public final class ForeignKeyMapper {

  /**
   * Enriches foreign keys with nullability information from columns.
   *
   * @param rawForeignKeys the raw foreign keys from database metadata
   * @param columns the columns containing nullability information
   * @return list of {@link ForeignKey} instances with nullability enriched
   */
  public List<ForeignKey> enrichWithNullability(
      final List<ForeignKey> rawForeignKeys, final List<Column> columns) {
    return rawForeignKeys.stream()
        .map(
            foreignKey -> {
              final var optionalColumn =
                  columns.stream()
                      .filter(column -> column.name().equals(foreignKey.sourceColumn()))
                      .findFirst();

              if (optionalColumn.isEmpty()) {
                return foreignKey;
              }

              return ForeignKey.builder()
                  .name(foreignKey.name())
                  .sourceTable(foreignKey.sourceTable())
                  .sourceColumn(foreignKey.sourceColumn())
                  .targetTable(foreignKey.targetTable())
                  .targetColumn(foreignKey.targetColumn())
                  .referencedSchema(foreignKey.referencedSchema())
                  .isNullable(optionalColumn.get().isNullable())
                  .build();
            })
        .toList();
  }
}
