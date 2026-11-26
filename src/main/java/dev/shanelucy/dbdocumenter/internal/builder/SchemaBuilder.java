package db.documenter.internal.builder;

import db.documenter.internal.models.db.Schema;
import java.sql.SQLException;
import java.util.List;

/**
 * Contract for building database schema documentation from connection metadata.
 *
 * <p>One implementation exists per supported database; each orchestrates the table and enum
 * builders to produce the {@link Schema} instances that are rendered into the ERD and enum
 * diagrams.
 *
 * @see PostgresqlSchemaBuilder
 * @see Schema
 */
// A per-database strategy, not a lambda target: @FunctionalInterface would forbid adding methods.
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface SchemaBuilder {

  /**
   * Builds a list of schemas with their tables and enums.
   *
   * @param schemaNames the list of schema names to build
   * @return list of {@link Schema} instances
   * @throws SQLException if database access fails
   */
  List<Schema> buildSchemas(List<String> schemaNames) throws SQLException;
}
