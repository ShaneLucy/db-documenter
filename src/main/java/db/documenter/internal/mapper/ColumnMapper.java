package db.documenter.internal.mapper;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Maps raw column metadata to enriched column instances. */
public final class ColumnMapper {

  /**
   * Maps USER-DEFINED column types to their corresponding enum types.
   *
   * @param rawColumns the raw columns from database metadata
   * @param dbEnums the list of database enums for type mapping
   * @return list of {@link Column} instances with enum types resolved
   */
  public List<Column> mapUserDefinedTypes(
      final List<Column> rawColumns, final List<DbEnum> dbEnums) {
    return rawColumns.stream()
        .map(
            column -> {
              if (!Objects.equals(column.dataType(), "USER-DEFINED")) {
                return column;
              }

              final var dataType =
                  dbEnums.stream()
                      .filter(dbEnum -> dbEnum.columnName().equals(column.name()))
                      .findFirst()
                      .map(DbEnum::enumName)
                      .orElse(column.dataType());

              return Column.builder()
                  .name(column.name())
                  .dataType(dataType)
                  .maximumLength(column.maximumLength())
                  .constraints(column.constraints())
                  .build();
            })
        .toList();
  }

  /**
   * Enriches columns with FK constraint for columns that are foreign keys.
   *
   * @param columns the columns to enrich
   * @param foreignKeys the foreign keys for the table
   * @return list of {@link Column} instances with FK constraint added where applicable
   */
  public List<Column> enrichWithForeignKeyConstraints(
      final List<Column> columns, final List<ForeignKey> foreignKeys) {
    return columns.stream()
        .map(
            column -> {
              final var isForeignKey =
                  foreignKeys.stream()
                      .anyMatch(fk -> fk.sourceColumn().equalsIgnoreCase(column.name()));

              if (!isForeignKey) {
                return column;
              }

              final List<Constraint> updatedConstraints = new ArrayList<>();
              updatedConstraints.add(Constraint.FK);
              if (column.constraints() != null) {
                updatedConstraints.addAll(column.constraints());
              }

              return Column.builder()
                  .name(column.name())
                  .dataType(column.dataType())
                  .maximumLength(column.maximumLength())
                  .constraints(updatedConstraints)
                  .build();
            })
        .toList();
  }
}
