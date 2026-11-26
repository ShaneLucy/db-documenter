package dev.shanelucy.dbdocumenter.internal.queries.impl.postgresql.resultsets;

import dev.shanelucy.dbdocumenter.internal.models.db.ColumnKey;
import dev.shanelucy.dbdocumenter.internal.models.db.DbEnum;
import dev.shanelucy.dbdocumenter.internal.models.db.postgresql.UdtReference;
import dev.shanelucy.dbdocumenter.internal.queries.AbstractResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL-specific implementation of {@link
 * dev.shanelucy.dbdocumenter.internal.queries.api.ResultSetMapper}.
 *
 * <p>Extracts column values from PostgreSQL metadata result sets and constructs immutable domain
 * records. Table stub objects populate only the name; columns are enriched by the builder layer in
 * a subsequent query.
 *
 * <p>All four generic mapping methods ({@code mapToTables}, {@code mapToPrimaryKey}, {@code
 * mapToForeignKeys}, {@code mapToColumns}) are inherited from {@link AbstractResultSetMapper}. This
 * class provides PostgreSQL-specific mapping methods for enums and UDT mappings.
 */
public final class PostgresqlResultSetMapper extends AbstractResultSetMapper {

  /**
   * Maps result set rows to {@link DbEnum} stub objects containing the schema and name.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of enum stubs (values empty); never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  public List<DbEnum> mapToDbEnumInfo(final ResultSet resultSet) throws SQLException {
    final List<DbEnum> dbEnums = new ArrayList<>();

    while (resultSet.next()) {
      final var udtSchema = resultSet.getString("udt_schema");
      final var udtName = resultSet.getString("udt_name");

      dbEnums.add(
          DbEnum.builder().schemaName(udtSchema).enumName(udtName).enumValues(List.of()).build());
    }

    return dbEnums;
  }

  /**
   * Maps result set rows to ordered enum label strings.
   *
   * @param resultSet the result set positioned before the first row
   * @return ordered list of enum value labels; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  public List<String> mapToDbEnumValues(final ResultSet resultSet) throws SQLException {
    final List<String> dbEnumValues = new ArrayList<>();

    while (resultSet.next()) {
      dbEnumValues.add(resultSet.getString("enumlabel"));
    }

    return dbEnumValues;
  }

  // ColumnKey and UdtReference are only created when a new mapping is encountered.
  // This is standard Java pattern for mapping database rows to domain objects.
  // PMD wants ConcurrentHashMap, but this is a local variable with no concurrent access.
  // LinkedHashMap is correct for preserving insertion order.
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseConcurrentHashMap"})
  public Map<ColumnKey, UdtReference> mapToColumnUdtMappings(final ResultSet resultSet)
      throws SQLException {
    final Map<ColumnKey, UdtReference> mappings = new LinkedHashMap<>();

    while (resultSet.next()) {
      final var tableName = resultSet.getString("table_name");
      final var columnName = resultSet.getString("column_name");
      final var udtSchema = resultSet.getString("udt_schema");
      final var udtName = resultSet.getString("udt_name");

      final var columnKey = new ColumnKey(tableName, columnName);
      final var udtReference = new UdtReference(udtSchema, udtName);

      mappings.put(columnKey, udtReference);
    }

    return mappings;
  }
}
