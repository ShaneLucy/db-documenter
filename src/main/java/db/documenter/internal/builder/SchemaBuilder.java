package db.documenter.internal.builder;

import db.documenter.internal.models.db.Schema;
import java.sql.SQLException;
import java.util.List;

/**
 * Contract for building database schema documentation from connection metadata.
 *
 * <p>Implementations orchestrate table, view, enum, composite type, and materialized view builders
 * to produce a complete list of {@link Schema} instances. Each schema aggregates all discovered
 * database objects for use in documentation generation.
 *
 * @see PostgresqlSchemaBuilder
 * @see Schema
 */
public interface SchemaBuilder {

  /**
   * Builds a list of schemas with their tables, views, materialized views, enums, and composite
   * types.
   *
   * @param schemaNames the list of schema names to build
   * @return list of {@link Schema} instances
   * @throws SQLException if database access fails
   */
  List<Schema> buildSchemas(List<String> schemaNames) throws SQLException;
}
