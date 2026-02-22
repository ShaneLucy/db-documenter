package db.documenter.internal.builder;

import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import db.documenter.internal.utils.LogUtils;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds database composite type information from schema metadata.
 *
 * <p>This builder queries the database to retrieve composite type definitions (created via {@code
 * CREATE TYPE ... AS (...)}) including their field names, types, and positions. Composite types are
 * user-defined structured types that can be used in column definitions, function returns, or other
 * type contexts.
 *
 * <p><b>Database Support:</b> Currently supports PostgreSQL composite types queried from {@code
 * pg_catalog} views.
 *
 * <p><b>Usage Pattern:</b> This builder is invoked by {@link SchemaBuilder} during the schema
 * building process, after enums but before tables are constructed.
 *
 * @see DbCompositeType
 * @see SchemaBuilder
 */
public final class CompositeTypeBuilder {
  private static final Logger LOGGER = Logger.getLogger(CompositeTypeBuilder.class.getName());

  /**
   * Builds a list of database composite types for a given schema.
   *
   * <p>Queries the database to retrieve all composite type definitions and their constituent fields
   * for the specified schema. Each composite type includes its name, schema, and ordered list of
   * fields with their types.
   *
   * @param queryRunner the query runner to fetch composite type metadata
   * @param schema the schema name to query for composite types
   * @return immutable list of {@link DbCompositeType} instances; never null, may be empty if no
   *     composite types exist in the schema
   * @throws SQLException if database access fails or query execution errors occur
   */
  public List<DbCompositeType> buildCompositeTypes(
      final PostgresqlQueryRunner queryRunner, final String schema) throws SQLException {

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(
          Level.INFO, "Building composite types for schema: {0}", LogUtils.sanitizeForLog(schema));
    }

    final List<DbCompositeType> compositeTypes = queryRunner.getCompositeTypeInfo(schema);

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.log(
          Level.FINE,
          "Found {0} composite type(s) in schema: {1}",
          new Object[] {compositeTypes.size(), LogUtils.sanitizeForLog(schema)});
    }

    return compositeTypes;
  }
}
