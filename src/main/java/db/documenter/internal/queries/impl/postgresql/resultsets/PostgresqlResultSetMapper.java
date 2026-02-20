package db.documenter.internal.queries.impl.postgresql.resultsets;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.CompositeField;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.MaterializedView;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.View;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.api.ResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * PostgreSQL-specific implementation of {@link ResultSetMapper}.
 *
 * <p>Extracts column values from PostgreSQL metadata result sets and constructs immutable domain
 * records. Methods that return stub objects (tables, views, materialized views) populate only the
 * name; columns are enriched by the builder layer in a subsequent query.
 */
public final class PostgresqlResultSetMapper implements ResultSetMapper {

  @Override
  public List<Table> mapToTables(final ResultSet resultSet) throws SQLException {
    final List<Table> tables = new ArrayList<>();
    while (resultSet.next()) {
      final var partitionKey = resultSet.getString("partition_key");
      tables.add(
          Table.builder()
              .name(resultSet.getString("table_name"))
              .columns(List.of())
              .foreignKeys(List.of())
              .partitionStrategy(partitionKey)
              .build());
    }
    return tables;
  }

  @Override
  public List<Column> mapToColumns(final ResultSet resultSet) throws SQLException {
    final List<Column> columns = new ArrayList<>();
    while (resultSet.next()) {
      final List<Constraint> constraints = buildConstraints(resultSet);
      final String dataType = resolveDataType(resultSet);

      columns.add(
          Column.builder()
              .name(resultSet.getString("column_name"))
              .dataType(dataType)
              .maximumLength(resultSet.getInt("character_maximum_length"))
              .constraints(constraints)
              .build());
    }
    return columns;
  }

  /**
   * Resolves the display data type for a column, applying NUMERIC precision/scale formatting.
   *
   * <p>Array types are already converted to the {@code element_type[]} form by the SQL query.
   * NUMERIC columns with explicit precision and scale are formatted as {@code numeric(p,s)} to
   * preserve schema-defined constraints in the generated diagram.
   *
   * @param resultSet the current result set row
   * @return the resolved data type string for display
   * @throws SQLException if a database access error occurs
   */
  private String resolveDataType(final ResultSet resultSet) throws SQLException {
    final var dataType = resultSet.getString("data_type");
    final var isNumericDataType = "numeric".equals(dataType);

    if (isNumericDataType) {
      final var precision = resultSet.getObject("numeric_precision", Integer.class);
      final var scale = resultSet.getObject("numeric_scale", Integer.class);
      if (precision != null && scale != null) {
        return "numeric(" + precision + "," + scale + ")";
      }
    }

    return dataType;
  }

  private List<Constraint> buildConstraints(final ResultSet resultSet) throws SQLException {
    final List<Constraint> constraints = new ArrayList<>();

    if (resultSet.getBoolean("is_unique")) {
      constraints.add(Constraint.UNIQUE);
    }

    final var checkConstraint = resultSet.getString("check_constraint");
    if (checkConstraint != null && !checkConstraint.isBlank()) {
      constraints.add(Constraint.CHECK);
    }

    final var defaultValue = resultSet.getString("column_default");
    if (defaultValue != null && !defaultValue.isBlank()) {
      constraints.add(Constraint.DEFAULT);
    }

    if (resultSet.getBoolean("is_auto_increment")) {
      constraints.add(Constraint.AUTO_INCREMENT);
    }

    if (Objects.equals(resultSet.getString("is_nullable"), "YES")) {
      constraints.add(Constraint.NULLABLE);
    }

    if (Objects.equals(resultSet.getString("is_generated"), "ALWAYS")) {
      constraints.add(Constraint.GENERATED);
    }

    return constraints;
  }

  @Override
  public PrimaryKey mapToPrimaryKey(final ResultSet resultSet) throws SQLException {
    if (!resultSet.next()) {
      return null;
    }

    final var primaryKeyBuilder =
        PrimaryKey.builder().constraintName(resultSet.getString("constraint_name"));

    final List<String> columnNames = new ArrayList<>();
    columnNames.add(resultSet.getString("column_name"));

    while (resultSet.next()) {
      columnNames.add(resultSet.getString("column_name"));
    }

    return primaryKeyBuilder.columnNames(columnNames).build();
  }

  @Override
  public List<ForeignKey> mapToForeignKeys(final ResultSet resultSet) throws SQLException {
    final List<ForeignKey> foreignKeys = new ArrayList<>();

    while (resultSet.next()) {
      foreignKeys.add(
          ForeignKey.builder()
              .name(resultSet.getString("constraint_name"))
              .sourceTable(resultSet.getString("source_table_name"))
              .sourceColumn(resultSet.getString("source_column"))
              .targetTable(resultSet.getString("referenced_table"))
              .targetColumn(resultSet.getString("referenced_column"))
              .referencedSchema(resultSet.getString("referenced_schema"))
              .build());
    }

    return foreignKeys;
  }

  @Override
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

  @Override
  public List<String> mapToDbEnumValues(final ResultSet resultSet) throws SQLException {
    final List<String> dbEnumValues = new ArrayList<>();

    while (resultSet.next()) {
      dbEnumValues.add(resultSet.getString("enumlabel"));
    }

    return dbEnumValues;
  }

  @Override
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

  @Override
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

  @Override
  public List<View> mapToViews(final ResultSet resultSet) throws SQLException {
    final List<View> views = new ArrayList<>();
    while (resultSet.next()) {
      views.add(View.builder().name(resultSet.getString("table_name")).columns(List.of()).build());
    }
    return views;
  }

  @Override
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

  @Override
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
