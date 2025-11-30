package db.documenter.internal.models.db;

/**
 * Enum representing supported relational database management systems.
 *
 * <p>This enum is used to determine which database-specific implementation to use for connection
 * management and query execution.
 *
 * <p><b>Currently Supported:</b>
 *
 * <ul>
 *   <li>{@link #POSTGRESQL} - PostgreSQL database
 * </ul>
 *
 * <p><b>Adding New Database Support:</b> To add support for a new database type:
 *
 * <ol>
 *   <li>Add a new enum constant here
 *   <li>Implement {@link db.documenter.internal.connection.api.ConnectionManager} for the database
 *   <li>Implement {@link db.documenter.internal.queries.api.QueryRunner} for the database
 *   <li>Update {@link db.documenter.internal.connection.ConnectionManagerFactory}
 *   <li>Update {@link db.documenter.internal.queries.QueryRunnerFactory}
 * </ol>
 *
 * @see db.documenter.DbDocumenterConfig
 */
public enum RdbmsTypes {
  /** PostgreSQL database system. */
  POSTGRESQL
}
