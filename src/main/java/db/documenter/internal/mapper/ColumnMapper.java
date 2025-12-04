package db.documenter.internal.mapper;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure transformation mapper for enriching column metadata.
 *
 * <p>This mapper provides stateless transformation functions that enrich raw column data with
 * additional metadata.
 *
 * <p><b>Design Pattern:</b> Stateless transformer - all methods are pure functions with no side
 * effects.
 *
 * <p><b>Responsibilities:</b>
 *
 * <ul>
 *   <li>Map USER-DEFINED types to their corresponding enum type names
 *   <li>Add FK constraint to columns that are foreign keys
 * </ul>
 *
 * <p><b>Usage Example:</b>
 *
 * <pre>{@code
 * ColumnMapper mapper = new ColumnMapper();
 *
 * // Map USER-DEFINED types to enum names
 * List<Column> enrichedColumns = mapper.mapUserDefinedTypes(rawColumns, dbEnums);
 *
 * // Add FK constraint to foreign key columns
 * List<Column> finalColumns = mapper.enrichWithForeignKeyConstraints(
 *     enrichedColumns, foreignKeys);
 * }</pre>
 *
 * @see Column
 */
public final class ColumnMapper {

  /**
   * Maps USER-DEFINED column types to their corresponding enum type names.
   *
   * <p>In PostgreSQL, custom enum types (created with {@code CREATE TYPE}) appear as "USER-DEFINED"
   * in the data type field. This method resolves the actual enum type name by matching the column
   * name against the list of database enums.
   *
   * <p><b>Transformation Logic:</b> For each column with dataType "USER-DEFINED", find a matching
   * DbEnum by column name and replace the dataType with the enum's type name. If no match is found,
   * the column is returned unchanged.
   *
   * <p><b>Usage Example:</b>
   *
   * <pre>{@code
   * // Raw column from database: dataType = "USER-DEFINED"
   * Column statusColumn = Column.builder()
   *     .name("status")
   *     .dataType("USER-DEFINED")
   *     .constraints(List.of())
   *     .build();
   *
   * DbEnum orderStatusEnum = DbEnum.builder()
   *     .enumName("order_status")
   *     .columnName("status")
   *     .enumValues(List.of("PENDING", "SHIPPED"))
   *     .build();
   *
   * // After mapping: dataType = "order_status"
   * List<Column> result = mapper.mapUserDefinedTypes(
   *     List.of(statusColumn),
   *     List.of(orderStatusEnum)
   * );
   * }</pre>
   *
   * @param rawColumns the raw columns from database metadata
   * @param dbEnums the list of {@link DbEnum} instances for type mapping
   * @return list of {@link Column} instances with USER-DEFINED types resolved to enum type names
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
                      .filter(dbEnum -> dbEnum.columnNames().contains(column.name()))
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
              updatedConstraints.addAll(column.constraints());

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
