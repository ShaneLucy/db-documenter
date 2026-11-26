package db.documenter.internal.queries;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.api.ResultSetMapper;
import db.documenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Sealed base class for all database-specific result set mapper implementations.
 *
 * <p>Provides concrete implementations for all generic {@link ResultSetMapper} methods whose
 * mapping logic is portable across ANSI SQL-compatible engines:
 *
 * <ul>
 *   <li>{@link #mapToTables(ResultSet)} — reads {@code table_name}
 *   <li>{@link #mapToPrimaryKey(ResultSet)} — reads {@code constraint_name} and {@code column_name}
 *   <li>{@link #mapToForeignKeys(ResultSet)} — reads ANSI {@code information_schema} FK columns
 *   <li>{@link #mapToColumns(ResultSet)} — reads ANSI {@code information_schema.columns} fields
 * </ul>
 *
 * <p>The {@code sealed} modifier restricts the class hierarchy to known implementations, enabling
 * exhaustive pattern matching and preventing unexpected extension.
 *
 * @see ResultSetMapper
 * @see PostgresqlResultSetMapper
 */
public abstract sealed class AbstractResultSetMapper implements ResultSetMapper
    permits PostgresqlResultSetMapper {

  private static final String NUMERIC_DATA_TYPE = "numeric";

  @Override
  public List<Table> mapToTables(final ResultSet resultSet) throws SQLException {
    final List<Table> tables = new ArrayList<>();
    while (resultSet.next()) {
      tables.add(
          Table.builder()
              .name(resultSet.getString("table_name"))
              .columns(List.of())
              .foreignKeys(List.of())
              .build());
    }
    return tables;
  }

  @Override
  public Optional<PrimaryKey> mapToPrimaryKey(final ResultSet resultSet) throws SQLException {
    if (!resultSet.next()) {
      return Optional.empty();
    }

    final var primaryKeyBuilder =
        PrimaryKey.builder().constraintName(resultSet.getString("constraint_name"));

    final List<String> columnNames = new ArrayList<>();
    columnNames.add(resultSet.getString("column_name"));

    while (resultSet.next()) {
      columnNames.add(resultSet.getString("column_name"));
    }

    return Optional.of(primaryKeyBuilder.columnNames(columnNames).build());
  }

  @Override
  public List<Column> mapToColumns(final ResultSet resultSet) throws SQLException {
    final List<Column> columns = new ArrayList<>();
    while (resultSet.next()) {
      final List<Constraint> constraints = buildConstraints(resultSet);
      final String dataType = resolveDataType(resultSet);
      final boolean isNullable = Objects.equals(resultSet.getString("is_nullable"), "YES");

      columns.add(
          Column.builder()
              .name(resultSet.getString("column_name"))
              .dataType(dataType)
              .maximumLength(resultSet.getInt("character_maximum_length"))
              .isNullable(isNullable)
              .constraints(constraints)
              .build());
    }
    return columns;
  }

  /**
   * Resolves the display data type for a column, applying NUMERIC precision/scale formatting.
   *
   * <p>NUMERIC columns with explicit precision and scale are formatted as {@code numeric(p,s)} to
   * preserve schema-defined constraints in the generated diagram. All other types are returned
   * verbatim from the result set.
   *
   * @param resultSet the current result set row
   * @return the resolved data type string for display
   * @throws SQLException if a database access error occurs
   */
  protected String resolveDataType(final ResultSet resultSet) throws SQLException {
    final var dataType = resultSet.getString("data_type");
    if (NUMERIC_DATA_TYPE.equals(dataType)) {
      final var precision = resultSet.getObject("numeric_precision", Integer.class);
      final var scale = resultSet.getObject("numeric_scale", Integer.class);
      if (precision != null && scale != null) {
        return "numeric(" + precision + "," + scale + ")";
      }
    }
    return dataType;
  }

  /**
   * Builds the list of ERD-relevant {@link Constraint} values for a column from the current result
   * set row.
   *
   * <p>Only structural constraints ({@link Constraint#UNIQUE}) are collected here. Nullability is
   * read as a dedicated boolean field; physical constraints (CHECK, DEFAULT, AUTO_INCREMENT,
   * GENERATED) are not included in ERD output.
   *
   * @param resultSet the current result set row
   * @return list of constraints for the column; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  protected List<Constraint> buildConstraints(final ResultSet resultSet) throws SQLException {
    final List<Constraint> constraints = new ArrayList<>();

    if (resultSet.getBoolean("is_unique")) {
      constraints.add(Constraint.UNIQUE);
    }

    return constraints;
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
              // Placeholder: ForeignKeyMapper.enrichWithNullability sets the real value from the
              // source column.
              .isNullable(false)
              .build());
    }

    return foreignKeys;
  }
}
