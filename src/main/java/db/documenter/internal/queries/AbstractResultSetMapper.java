package db.documenter.internal.queries;

import static db.documenter.internal.utils.LogUtils.sanitizeForLog;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Constraint;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.ReferentialAction;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.View;
import db.documenter.internal.queries.api.ResultSetMapper;
import db.documenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sealed base class for all database-specific result set mapper implementations.
 *
 * <p>Provides concrete implementations for all five generic {@link ResultSetMapper} methods whose
 * mapping logic is portable across ANSI SQL-compatible engines:
 *
 * <ul>
 *   <li>{@link #mapToTables(ResultSet)} — reads {@code table_name} and {@code partition_key}
 *   <li>{@link #mapToPrimaryKey(ResultSet)} — reads {@code constraint_name} and {@code column_name}
 *   <li>{@link #mapToViews(ResultSet)} — reads {@code table_name}
 *   <li>{@link #mapToForeignKeys(ResultSet)} — reads ANSI {@code information_schema} referential
 *       action strings ({@code NO ACTION}, {@code CASCADE}, etc.)
 *   <li>{@link #mapToColumns(ResultSet)} — reads ANSI {@code information_schema.columns} fields;
 *       engine-specific subclasses may override for database-specific query shapes
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

  private static final Logger LOGGER = Logger.getLogger(AbstractResultSetMapper.class.getName());

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
  public List<View> mapToViews(final ResultSet resultSet) throws SQLException {
    final List<View> views = new ArrayList<>();
    while (resultSet.next()) {
      views.add(View.builder().name(resultSet.getString("table_name")).columns(List.of()).build());
    }
    return views;
  }

  @Override
  public List<Column> mapToColumns(final ResultSet resultSet) throws SQLException {
    final List<Column> columns = new ArrayList<>();
    while (resultSet.next()) {
      final List<Constraint> constraints = buildConstraints(resultSet);
      final String dataType = resolveDataType(resultSet);

      final var compositeUniqueConstraintName =
          resultSet.getString("composite_unique_constraint_name");
      columns.add(
          Column.builder()
              .name(resultSet.getString("column_name"))
              .dataType(dataType)
              .maximumLength(resultSet.getInt("character_maximum_length"))
              .constraints(constraints)
              .compositeUniqueConstraintName(compositeUniqueConstraintName)
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
    if ("numeric".equals(dataType)) {
      final var precision = resultSet.getObject("numeric_precision", Integer.class);
      final var scale = resultSet.getObject("numeric_scale", Integer.class);
      if (precision != null && scale != null) {
        return "numeric(" + precision + "," + scale + ")";
      }
    }
    return dataType;
  }

  /**
   * Builds the list of {@link Constraint} values for a column from the current result set row.
   *
   * <p>Reads {@code is_unique}, {@code check_constraint}, {@code column_default}, {@code
   * is_auto_increment}, {@code is_nullable}, and {@code is_generated} — all of which are present in
   * both the ANSI and engine-specific column queries.
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
  public List<ForeignKey> mapToForeignKeys(final ResultSet resultSet) throws SQLException {
    final List<ForeignKey> foreignKeys = new ArrayList<>();

    while (resultSet.next()) {
      final var onDeleteAction = decodeReferentialAction(resultSet.getString("on_delete_type"));
      final var onUpdateAction = decodeReferentialAction(resultSet.getString("on_update_type"));
      foreignKeys.add(
          ForeignKey.builder()
              .name(resultSet.getString("constraint_name"))
              .sourceTable(resultSet.getString("source_table_name"))
              .sourceColumn(resultSet.getString("source_column"))
              .targetTable(resultSet.getString("referenced_table"))
              .targetColumn(resultSet.getString("referenced_column"))
              .referencedSchema(resultSet.getString("referenced_schema"))
              .onDeleteAction(onDeleteAction)
              .onUpdateAction(onUpdateAction)
              .build());
    }

    return foreignKeys;
  }

  /**
   * Decodes an ANSI SQL referential action string from {@code
   * information_schema.referential_constraints} into the corresponding {@link ReferentialAction}.
   *
   * <p>Unknown values are logged at WARNING level and fall back to {@link
   * ReferentialAction#NO_ACTION} to avoid failing the entire schema load for an unrecognised value.
   *
   * @param action the referential action string ({@code NO ACTION}, {@code CASCADE}, etc.), may be
   *     null
   * @return the decoded {@link ReferentialAction}; never null
   */
  private ReferentialAction decodeReferentialAction(final String action) {
    if (action == null) {
      return ReferentialAction.NO_ACTION;
    }
    return switch (action) {
      case "NO ACTION" -> ReferentialAction.NO_ACTION;
      case "RESTRICT" -> ReferentialAction.RESTRICT;
      case "CASCADE" -> ReferentialAction.CASCADE;
      case "SET NULL" -> ReferentialAction.SET_NULL;
      case "SET DEFAULT" -> ReferentialAction.SET_DEFAULT;
      default -> {
        if (LOGGER.isLoggable(Level.WARNING)) {
          LOGGER.log(Level.WARNING, "Unknown referential action: {0}", sanitizeForLog(action));
        }
        yield ReferentialAction.NO_ACTION;
      }
    };
  }
}
