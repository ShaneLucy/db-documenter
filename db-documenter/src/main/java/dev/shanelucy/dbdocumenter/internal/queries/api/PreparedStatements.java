package dev.shanelucy.dbdocumenter.internal.queries.api;

import dev.shanelucy.dbdocumenter.internal.queries.SqlAgnosticPreparedStatements;
import dev.shanelucy.dbdocumenter.internal.queries.impl.postgresql.PostgresqlPreparedStatements;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Contract for providing prepared statements used to extract database schema metadata.
 *
 * @see SqlAgnosticPreparedStatements
 * @see PostgresqlPreparedStatements
 */
public sealed interface PreparedStatements permits SqlAgnosticPreparedStatements {

  PreparedStatement tableInfoPreparedStatement(Connection connection) throws SQLException;

  PreparedStatement columnInfoPreparedStatement(Connection connection) throws SQLException;

  PreparedStatement primaryKeyInfoPreparedStatement(Connection connection) throws SQLException;

  PreparedStatement foreignKeyInfoPreparedStatement(Connection connection) throws SQLException;
}
