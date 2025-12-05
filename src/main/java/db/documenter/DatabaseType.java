package db.documenter;

import org.jspecify.annotations.NonNull;

/**
 * Defines supported database systems and their JDBC connection characteristics.
 *
 * <p>Each database type encapsulates the JDBC URL template format required for establishing
 * connections. The URL template uses standard {@link String#format(String, Object...)} placeholders
 * (%s for strings, %d for integers) to construct JDBC connection strings from host, port, and
 * database name parameters.
 *
 * <p><b>Thread Safety:</b> This enum is immutable and thread-safe.
 *
 * <p><b>Adding New Database Support:</b> To extend db-documenter for additional database systems,
 * implement three components:
 *
 * <ul>
 *   <li>Add a new enum constant here with appropriate display name and JDBC URL template
 *   <li>Implement a ConnectionManager for database-specific connection handling
 *   <li>Implement a QueryRunner for database-specific metadata queries
 * </ul>
 *
 * @see db.documenter.DbDocumenterConfig
 */
public enum DatabaseType {

  /**
   * PostgreSQL database system.
   *
   * <p>Uses JDBC URL format: {@code jdbc:postgresql://host:port/database}
   */
  POSTGRESQL("PostgreSQL", "jdbc:postgresql://%s:%d/%s");

  private final @NonNull String displayName;
  private final @NonNull String urlTemplate;

  /**
   * Creates a database type with its display name and JDBC URL template.
   *
   * @param displayName human-readable name for this database type
   * @param urlTemplate JDBC URL format string with placeholders for host (%s), port (%d), and
   *     database name (%s) in that order
   */
  DatabaseType(final @NonNull String displayName, final @NonNull String urlTemplate) {
    this.displayName = displayName;
    this.urlTemplate = urlTemplate;
  }

  /**
   * Returns the human-readable name for this database type.
   *
   * @return display name such as "PostgreSQL"; never null
   */
  public @NonNull String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the JDBC URL template for constructing connection strings.
   *
   * <p>The template contains {@link String#format(String, Object...)} placeholders that should be
   * filled with host (String), port (int), and database name (String) in that order.
   *
   * <p><b>Example usage:</b>
   *
   * <pre>{@code
   * String jdbcUrl = String.format(
   *     DatabaseType.POSTGRESQL.getUrlTemplate(),
   *     "localhost",
   *     5432,
   *     "mydb"
   * );
   * // Result: "jdbc:postgresql://localhost:5432/mydb"
   * }</pre>
   *
   * @return JDBC URL format string; never null
   */
  public @NonNull String getUrlTemplate() {
    return urlTemplate;
  }
}
