package dev.shanelucy.dbdocumenter.internal.reader;

import dev.shanelucy.dbdocumenter.catalog.CatalogSnapshot;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Reads the catalog metadata of the requested schemas from one database vendor.
 *
 * <p><b>Contract:</b>
 *
 * <ul>
 *   <li>The caller owns the connection: {@link #read} never closes it, and never changes its
 *       autocommit mode, transaction isolation or schema search path ({@code search_path} on
 *       PostgreSQL). Statements and result sets it opens are closed before it returns.
 *   <li>The snapshot contains only requested schemas that exist and are visible to the connected
 *       user; a missing or invisible schema is silently absent, not an error.
 *   <li>Rows that become orphans through concurrent DDL between the reader's queries (for example a
 *       column whose table was dropped in between) are dropped and logged, not thrown.
 *   <li>Implementations are stateless and thread-safe, so one instance may serve many connections
 *       concurrently. A single connection must still be used by one thread at a time, as JDBC
 *       requires.
 * </ul>
 *
 * @see MetadataReaders
 * @see ReadRequest
 */
// A service interface with one operation, not a callback; lambdas are fine in tests.
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface MetadataReader {

  /**
   * Reads a snapshot of the requested schemas.
   *
   * @param connection an open connection, which stays open and unmodified
   * @param request the schemas to read
   * @return the snapshot of the schemas that exist and are visible; its schema list may be empty
   * @throws SQLException if a metadata query fails
   */
  CatalogSnapshot read(Connection connection, ReadRequest request) throws SQLException;
}
