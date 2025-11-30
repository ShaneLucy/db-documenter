package db.documenter.internal.mapper;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.DbEnum;
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
                  .ordinalPosition(column.ordinalPosition())
                  .isNullable(column.isNullable())
                  .dataType(dataType)
                  .maximumLength(column.maximumLength())
                  .constraints(column.constraints())
                  .build();
            })
        .toList();
  }
}
