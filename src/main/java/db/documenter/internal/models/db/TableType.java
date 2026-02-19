package db.documenter.internal.models.db;

/**
 * Represents the type of a database object that can be documented as a table-like structure.
 *
 * <p>PostgreSQL distinguishes between regular tables, views (virtual tables defined by queries),
 * and materialized views (pre-computed query results stored physically). Each type has different
 * characteristics and is rendered with an appropriate stereotype in the generated PlantUML diagram.
 *
 * @see Table
 */
public enum TableType {
  /** A regular database table with physical storage. */
  TABLE,

  /** A view defined as a query; no physical storage. */
  VIEW,

  /** A materialized view with pre-computed, physically stored query results. */
  MATERIALIZED_VIEW
}
