package db.documenter.internal.queries.impl.postgresql.resultsets;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.CompositeField;
import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.MaterializedView;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.AbstractResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL-specific implementation of {@link db.documenter.internal.queries.api.ResultSetMapper}.
 *
 * <p>Extracts column values from PostgreSQL metadata result sets and constructs immutable domain
 * records. Methods that return stub objects (tables, views, materialized views) populate only the
 * name; columns are enriched by the builder layer in a subsequent query.
 *
 * <p>All five generic mapping methods ({@code mapToTables}, {@code mapToPrimaryKey}, {@code
 * mapToViews}, {@code mapToForeignKeys}, {@code mapToColumns}) are inherited from {@link
 * AbstractResultSetMapper}. This class provides the seven PostgreSQL-specific mapping methods for
 * enums, UDT mappings, composite types, materialized views, and partition children.
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

  // CompositeField objects are created for each attribute row.
  // LinkedHashMap preserves insertion order and is not accessed concurrently.
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseConcurrentHashMap"})
  public List<DbCompositeType> mapToDbCompositeTypeInfo(final ResultSet resultSet)
      throws SQLException {
    final Map<String, DbCompositeType.Builder> compositeTypeBuilders = new LinkedHashMap<>();

    while (resultSet.next()) {
      final var schemaName = resultSet.getString("schema_name");
      final var typeName = resultSet.getString("type_name");
      final var attributeName = resultSet.getString("attribute_name");
      final var attributeType = resultSet.getString("attribute_type");
      final var attributePosition = resultSet.getInt("attribute_position");

      final var typeKey = schemaName + "." + typeName;

      compositeTypeBuilders.computeIfAbsent(
          typeKey,
          k -> {
            final var builder = DbCompositeType.builder().schemaName(schemaName).typeName(typeName);
            builder.fields(new ArrayList<>());
            return builder;
          });

      if (attributeName != null) {
        final var field =
            CompositeField.builder()
                .fieldName(attributeName)
                .fieldType(attributeType)
                .position(attributePosition)
                .build();

        final var builder = compositeTypeBuilders.get(typeKey);
        final var currentFields = new ArrayList<>(builder.build().fields());
        currentFields.add(field);
        builder.fields(currentFields);
      }
    }

    return compositeTypeBuilders.values().stream().map(DbCompositeType.Builder::build).toList();
  }

  /**
   * Maps result set rows to stub {@link db.documenter.internal.models.db.MaterializedView} objects
   * containing only the view name.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of materialized view stubs (columns empty); never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  public List<MaterializedView> mapToMaterializedViews(final ResultSet resultSet)
      throws SQLException {
    final List<MaterializedView> materializedViews = new ArrayList<>();
    while (resultSet.next()) {
      materializedViews.add(
          MaterializedView.builder()
              .name(resultSet.getString("table_name"))
              .columns(List.of())
              .build());
    }
    return materializedViews;
  }

  /**
   * Maps result set rows for a materialized view to {@link Column} objects.
   *
   * <p>Delegates to {@link #mapToColumns(ResultSet)} since materialized view columns share the same
   * result set shape as regular table columns when queried via the pg_catalog path.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of columns; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  public List<Column> mapToMaterializedViewColumns(final ResultSet resultSet) throws SQLException {
    return mapToColumns(resultSet);
  }

  // Partition names are created for each row as the result set is iterated.
  // LinkedHashMap preserves insertion order (parent table declaration order) and is not
  // accessed concurrently.
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.UseConcurrentHashMap"})
  public Map<String, List<String>> mapToPartitionChildren(final ResultSet resultSet)
      throws SQLException {
    final Map<String, List<String>> result = new LinkedHashMap<>();
    while (resultSet.next()) {
      final var tableName = resultSet.getString("table_name");
      final var partitionName = resultSet.getString("partition_name");
      result.computeIfAbsent(tableName, k -> new ArrayList<>()).add(partitionName);
    }
    return result;
  }
}
