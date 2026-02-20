package db.documenter.internal.models.db;

/**
 * Internal classification enum for distinguishing database object types during SQL result mapping.
 *
 * <p>This enum is used as an internal routing mechanism when a single query result set contains
 * rows for multiple database object kinds (tables, views, materialized views) and those rows need
 * to be directed into the appropriate typed collections. It is <em>not</em> stored as a field on
 * any domain model record — {@link Table}, {@link View}, and {@link MaterializedView} are
 * intentionally separate types whose distinctions are encoded in the type system, not in a
 * discriminator field.
 *
 * <p><b>Example use case:</b> A future implementation that queries a unified {@code pg_class}
 * result set and routes each row by {@code relkind} could use this enum to map character values
 * ({@code 'r'}, {@code 'v'}, {@code 'm'}) to a typed switch expression before constructing the
 * appropriate domain object.
 */
public enum TableType {
  /** A regular database table with physical storage. */
  TABLE,

  /** A view defined as a query; no physical storage. */
  VIEW,

  /** A materialized view with pre-computed, physically stored query results. */
  MATERIALIZED_VIEW
}
