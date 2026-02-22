package db.documenter.internal.builder;

import db.documenter.internal.mapper.ColumnMapper;
import db.documenter.internal.mapper.ColumnMappingContext;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.MaterializedView;
import db.documenter.internal.models.db.View;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.api.QueryRunner;
import db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import db.documenter.internal.utils.LogUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds database view and materialized view information from schema metadata.
 *
 * <p>This builder queries the database to retrieve view and materialized view definitions, then
 * enriches each with its column metadata. Unlike tables, views and materialized views in PostgreSQL
 * have no primary key or foreign key constraints — so no PK or FK queries are performed.
 *
 * <p><b>Column Enrichment:</b> Regular view columns are retrieved via {@link
 * QueryRunner#getColumnInfo(String, String)} ({@code information_schema.columns}). Materialized
 * view columns are retrieved via {@link QueryRunner#getMaterializedViewColumnInfo(String, String)}
 * ({@code pg_catalog.pg_attribute}), because PostgreSQL's {@code information_schema} excludes
 * materialized views. USER-DEFINED column types are resolved using the same {@link ColumnMapper}
 * logic used for tables.
 *
 * <p><b>Usage Pattern:</b> This builder is invoked by {@link SchemaBuilder} during schema
 * construction, after enums and composite types but alongside table building.
 *
 * @see View
 * @see MaterializedView
 * @see SchemaBuilder
 * @see ColumnMapper
 */
public final class ViewBuilder {

  private static final Logger LOGGER = Logger.getLogger(ViewBuilder.class.getName());

  private final ColumnMapper columnMapper;

  /**
   * Creates a new {@code ViewBuilder} with the given column mapper.
   *
   * @param columnMapper the mapper used to resolve USER-DEFINED column types to enum names
   */
  public ViewBuilder(final ColumnMapper columnMapper) {
    this.columnMapper = columnMapper;
  }

  /**
   * Builds a list of views with their columns for the given schema.
   *
   * <p>For each view discovered by {@link QueryRunner#getViewInfo(String)}, this method fetches its
   * columns and resolves any USER-DEFINED types to enum names. No primary key or foreign key
   * queries are performed because views cannot have those constraints in PostgreSQL.
   *
   * @param queryRunner the query runner to fetch view and column metadata
   * @param schema the schema name to query
   * @param enumsByKey map of enum keys to enum definitions for O(1) UDT type lookup
   * @param columnUdtMappings map of column keys to UDT references for cross-schema resolution
   * @return immutable list of {@link View} instances with enriched columns; never null, may be
   *     empty if no views exist in the schema
   * @throws SQLException if database access fails or query execution errors occur
   */
  public List<View> buildViews(
      final QueryRunner queryRunner,
      final String schema,
      final Map<EnumKey, DbEnum> enumsByKey,
      final Map<ColumnKey, UdtReference> columnUdtMappings)
      throws SQLException {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "Building views for schema: {0}", LogUtils.sanitizeForLog(schema));
    }

    final List<View> stubViews = queryRunner.getViewInfo(schema);
    final List<View> result = new ArrayList<>();

    for (final View stubView : stubViews) {
      final List<Column> rawColumns = queryRunner.getColumnInfo(schema, stubView.name());

      // Context objects are intentionally created per-view for proper UDT resolution
      @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
      final ColumnMappingContext context =
          new ColumnMappingContext(columnUdtMappings, enumsByKey, stubView.name(), schema);

      final List<Column> columns = columnMapper.mapUserDefinedTypes(rawColumns, context);

      result.add(View.builder().name(stubView.name()).columns(columns).build());
    }

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.log(
          Level.FINE,
          "Found {0} view(s) in schema: {1}",
          new Object[] {result.size(), LogUtils.sanitizeForLog(schema)});
    }

    return result;
  }

  /**
   * Builds a list of materialized views with their columns for the given schema.
   *
   * <p>For each materialized view discovered by {@link
   * QueryRunner#getMaterializedViewInfo(String)}, this method fetches its columns via {@link
   * QueryRunner#getMaterializedViewColumnInfo(String, String)} and resolves any USER-DEFINED types
   * to enum names. {@code pg_catalog.pg_attribute} is used instead of {@code
   * information_schema.columns} because PostgreSQL's SQL-standard catalog view excludes
   * materialized views. No primary key or foreign key queries are performed because materialized
   * views cannot have those constraints in PostgreSQL.
   *
   * @param queryRunner the query runner to fetch materialized view and column metadata
   * @param schema the schema name to query
   * @param enumsByKey map of enum keys to enum definitions for O(1) UDT type lookup
   * @param columnUdtMappings map of column keys to UDT references for cross-schema resolution
   * @return immutable list of {@link MaterializedView} instances with enriched columns; never null,
   *     may be empty if no materialized views exist in the schema
   * @throws SQLException if database access fails or query execution errors occur
   */
  public List<MaterializedView> buildMaterializedViews(
      final PostgresqlQueryRunner queryRunner,
      final String schema,
      final Map<EnumKey, DbEnum> enumsByKey,
      final Map<ColumnKey, UdtReference> columnUdtMappings)
      throws SQLException {
    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(
          Level.INFO,
          "Building materialized views for schema: {0}",
          LogUtils.sanitizeForLog(schema));
    }

    final List<MaterializedView> stubMatViews = queryRunner.getMaterializedViewInfo(schema);
    final List<MaterializedView> result = new ArrayList<>();

    for (final MaterializedView stubMatView : stubMatViews) {
      final List<Column> rawColumns =
          queryRunner.getMaterializedViewColumnInfo(schema, stubMatView.name());

      // Context objects are intentionally created per-view for proper UDT resolution
      @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
      final ColumnMappingContext context =
          new ColumnMappingContext(columnUdtMappings, enumsByKey, stubMatView.name(), schema);

      final List<Column> columns = columnMapper.mapUserDefinedTypes(rawColumns, context);

      result.add(MaterializedView.builder().name(stubMatView.name()).columns(columns).build());
    }

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.log(
          Level.FINE,
          "Found {0} materialized view(s) in schema: {1}",
          new Object[] {result.size(), LogUtils.sanitizeForLog(schema)});
    }

    return result;
  }
}
