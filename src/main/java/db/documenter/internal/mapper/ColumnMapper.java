package db.documenter.internal.mapper;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.utils.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * // Create context for UDT resolution
 * ColumnMappingContext context = new ColumnMappingContext(
 *     columnUdtMappings, enumsByKey, "users", "core"
 * );
 *
 * // Map USER-DEFINED types to enum names
 * List<Column> enrichedColumns = mapper.mapUserDefinedTypes(rawColumns, context);
 *
 * // Add FK constraint to foreign key columns
 * List<Column> finalColumns = mapper.enrichWithForeignKeyConstraints(
 *     enrichedColumns, foreignKeys);
 * }</pre>
 *
 * @see Column
 */
public final class ColumnMapper {

  private static final Logger LOGGER = Logger.getLogger(ColumnMapper.class.getName());

  /**
   * Maps USER-DEFINED column types to their corresponding enum type names with cross-schema
   * resolution.
   *
   * <p>In PostgreSQL, custom enum types (created with {@code CREATE TYPE}) appear as "USER-DEFINED"
   * in the data type field. This method resolves the actual enum type name using the column mapping
   * context, which provides O(1) lookup of both UDT definitions and enum metadata.
   *
   * <p><b>Cross-Schema Resolution:</b> Uses the context's mappings to correctly handle enums
   * defined in different schemas than the tables using them.
   *
   * <p><b>Schema Qualification Rules:</b>
   *
   * <ul>
   *   <li>Same schema: Use unqualified name (e.g., {@code account_status})
   *   <li>Different schema: Use qualified name (e.g., {@code auth.account_status})
   *   <li>UDT not in documented schemas: Use qualified name with WARNING log
   * </ul>
   *
   * @param rawColumns the raw columns from database metadata
   * @param context the column mapping context containing UDT mappings and enum definitions
   * @return list of {@link Column} instances with USER-DEFINED types resolved to enum type names
   */
  // Complexity is necessary for cross-schema resolution and secure logging
  @SuppressWarnings("PMD.CognitiveComplexity")
  public List<Column> mapUserDefinedTypes(
      final List<Column> rawColumns, final ColumnMappingContext context) {
    return rawColumns.stream()
        .map(
            column -> {
              if (!Objects.equals(column.dataType(), "USER-DEFINED")) {
                return column;
              }

              final var columnKey = new ColumnKey(context.currentTableName(), column.name());
              final var udtReference = context.columnUdtMappings().get(columnKey);

              if (udtReference == null) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                  LOGGER.log(
                      Level.WARNING,
                      "No UDT mapping found for column {0}.{1}.{2}",
                      new Object[] {
                        LogUtils.sanitizeForLog(context.currentSchema()),
                        LogUtils.sanitizeForLog(context.currentTableName()),
                        LogUtils.sanitizeForLog(column.name())
                      });
                }
                return column;
              }

              final var enumKey = new EnumKey(udtReference.udtSchema(), udtReference.udtName());
              final var matchingEnum = context.enumsByKey().get(enumKey);

              final String dataType;
              if (matchingEnum != null) {

                if (context.currentSchema().equals(udtReference.udtSchema())) {

                  dataType = udtReference.udtName();
                } else {

                  dataType = udtReference.udtSchema() + "." + udtReference.udtName();

                  if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Cross-schema type reference: {0}.{1}.{2} -> {3}.{4}",
                        new Object[] {
                          LogUtils.sanitizeForLog(context.currentSchema()),
                          LogUtils.sanitizeForLog(context.currentTableName()),
                          LogUtils.sanitizeForLog(column.name()),
                          LogUtils.sanitizeForLog(udtReference.udtSchema()),
                          LogUtils.sanitizeForLog(udtReference.udtName())
                        });
                  }
                }
              } else {
                dataType = udtReference.udtSchema() + "." + udtReference.udtName();

                if (LOGGER.isLoggable(Level.WARNING)) {
                  LOGGER.log(
                      Level.WARNING,
                      "Column {0}.{1} references type {2}.{3} which is not in documented schemas",
                      new Object[] {
                        LogUtils.sanitizeForLog(context.currentTableName()),
                        LogUtils.sanitizeForLog(column.name()),
                        LogUtils.sanitizeForLog(udtReference.udtSchema()),
                        LogUtils.sanitizeForLog(udtReference.udtName())
                      });
                }
              }

              return Column.builder()
                  .name(column.name())
                  .dataType(dataType)
                  .maximumLength(column.maximumLength())
                  .constraints(column.constraints())
                  .compositeUniqueConstraintName(column.compositeUniqueConstraintName())
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
                  .compositeUniqueConstraintName(column.compositeUniqueConstraintName())
                  .build();
            })
        .toList();
  }
}
