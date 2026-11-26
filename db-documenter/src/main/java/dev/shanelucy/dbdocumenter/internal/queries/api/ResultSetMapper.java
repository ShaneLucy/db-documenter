package dev.shanelucy.dbdocumenter.internal.queries.api;

import dev.shanelucy.dbdocumenter.internal.models.db.Column;
import dev.shanelucy.dbdocumenter.internal.models.db.ForeignKey;
import dev.shanelucy.dbdocumenter.internal.models.db.PrimaryKey;
import dev.shanelucy.dbdocumenter.internal.models.db.Table;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Contract for mapping JDBC {@link ResultSet} rows to domain model objects.
 *
 * <p>Defines the four mapping operations shared across all supported database engines,
 * corresponding to the four methods in {@link QueryRunner}. Engine-specific mapping operations
 * (e.g., PostgreSQL enum values, UDT mappings) are declared on concrete subclasses and are not part
 * of this interface.
 *
 * <p>Implementations extract column values from the result set and construct immutable domain
 * records. Each method assumes the result set cursor is positioned before the first row (i.e.,
 * iteration starts from the beginning).
 *
 * <p><b>Note on stub objects:</b> {@link #mapToTables} returns objects with only the name populated
 * and an empty column list. Columns are enriched in a subsequent query by the builder layer,
 * following the two-phase construction pattern.
 *
 * @see dev.shanelucy.dbdocumenter.internal.queries.AbstractResultSetMapper
 * @see
 *     dev.shanelucy.dbdocumenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper
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
   * @return the primary key, or empty if the result set has no rows
   * @throws SQLException if a database access error occurs
   */
  Optional<PrimaryKey> mapToPrimaryKey(ResultSet resultSet) throws SQLException;

  /**
   * Maps result set rows to {@link ForeignKey} objects.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of foreign keys; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  List<ForeignKey> mapToForeignKeys(ResultSet resultSet) throws SQLException;
}
