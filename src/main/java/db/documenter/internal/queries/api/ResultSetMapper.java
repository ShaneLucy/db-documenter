package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.View;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Contract for mapping JDBC {@link ResultSet} rows to domain model objects.
 *
 * <p>Defines the five mapping operations shared across all supported database engines,
 * corresponding to the five methods in {@link QueryRunner}. Engine-specific mapping operations
 * (e.g., PostgreSQL enum values, composite types, materialized view columns) are declared on
 * concrete subclasses and are not part of this interface.
 *
 * <p>Implementations extract column values from the result set and construct immutable domain
 * records. Each method assumes the result set cursor is positioned before the first row (i.e.,
 * iteration starts from the beginning).
 *
 * <p><b>Note on stub objects:</b> {@link #mapToTables} and {@link #mapToViews} return objects with
 * only the name populated and an empty column list. Columns are enriched in a subsequent query by
 * the builder layer, following the two-phase construction pattern.
 *
 * @see db.documenter.internal.queries.AbstractResultSetMapper
 * @see db.documenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper
 */
public interface ResultSetMapper {

  /**
   * Maps result set rows to stub {@link Table} objects containing only the table name.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of stub tables; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  List<Table> mapToTables(ResultSet resultSet) throws SQLException;

  /**
   * Maps result set rows to {@link Column} objects.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of columns; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  List<Column> mapToColumns(ResultSet resultSet) throws SQLException;

  /**
   * Maps result set rows to a {@link PrimaryKey}.
   *
   * @param resultSet the result set positioned before the first row
   * @return the primary key, or null if the result set is empty
   * @throws SQLException if a database access error occurs
   */
  PrimaryKey mapToPrimaryKey(ResultSet resultSet) throws SQLException;

  /**
   * Maps result set rows to {@link ForeignKey} objects.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of foreign keys; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  List<ForeignKey> mapToForeignKeys(ResultSet resultSet) throws SQLException;

  /**
   * Maps result set rows to stub {@link View} objects containing only the view name.
   *
   * <p>Each row must contain a {@code table_name} column. The returned views have an empty column
   * list; columns are populated by the builder in a subsequent {@code getColumnInfo} call.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of stub views; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  List<View> mapToViews(ResultSet resultSet) throws SQLException;
}
