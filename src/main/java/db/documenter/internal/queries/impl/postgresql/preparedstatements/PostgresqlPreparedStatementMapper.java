package db.documenter.internal.queries.impl.postgresql.preparedstatements;

import db.documenter.internal.queries.AbstractPreparedStatementMapper;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * PostgreSQL-specific implementation of {@link
 * db.documenter.internal.queries.api.PreparedStatementMapper}.
 *
 * <p>Provides parameter binding for the PostgreSQL-specific metadata queries (enums, enum values,
 * and column UDT mappings). The four generic bindings (table info, column info, primary key info,
 * foreign key info) are inherited from {@link AbstractPreparedStatementMapper} and follow the
 * convention: schema is always parameter 1; the object name (table, enum name) is parameter 2 when
 * present.
 */
public final class PostgresqlPreparedStatementMapper extends AbstractPreparedStatementMapper {

  /**
   * Binds parameters for the enum info query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  public void prepareEnumInfoStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }

  /**
   * Binds parameters for the enum values query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @param enumName the name of the enum type
   * @throws SQLException if a database access error occurs
   */
  public void prepareEnumValuesStatement(
      final PreparedStatement preparedStatement, final String schema, final String enumName)
      throws SQLException {
    preparedStatement.setString(1, schema);
    preparedStatement.setString(2, enumName);
  }

  /**
   * Binds parameters for the column UDT mappings query.
   *
   * @param preparedStatement the statement to bind parameters on
   * @param schema the schema name
   * @throws SQLException if a database access error occurs
   */
  public void prepareColumnUdtMappingsStatement(
      final PreparedStatement preparedStatement, final String schema) throws SQLException {
    preparedStatement.setString(1, schema);
  }
}
