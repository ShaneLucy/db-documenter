package db.documenter.internal.queries.impl.postgresql;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.MaterializedView;
import db.documenter.internal.models.db.postgresql.UdtReference;
import db.documenter.internal.queries.AbstractQueryRunner;
import db.documenter.internal.queries.impl.postgresql.preparedstatements.PostgresqlPreparedStatementMapper;
import db.documenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper;
import db.documenter.internal.utils.LogUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PostgreSQL-specific implementation of {@link db.documenter.internal.queries.api.QueryRunner}.
 *
 * <p>Executes parameterized SQL queries against PostgreSQL {@code information_schema} and {@code
 * pg_catalog} views to retrieve schema metadata. Each method opens a prepared statement, binds
 * parameters, executes the query, and delegates result-set mapping to the injected {@link
 * PostgresqlResultSetMapper}.
 *
 * <p>The five generic methods ({@code getTableInfo}, {@code getColumnInfo}, {@code
 * getPrimaryKeyInfo}, {@code getForeignKeyInfo}, {@code getViewInfo}) are inherited from {@link
 * AbstractQueryRunner}. This class contributes the seven PostgreSQL-specific operations for enums,
 * UDT mappings, composite types, materialized views, materialized view columns, and partitions.
 *
 * <p><b>Resource Management:</b> All {@link java.sql.PreparedStatement} and {@link
 * java.sql.ResultSet} instances are managed with try-with-resources to guarantee closure even on
 * exceptions. The {@link Connection} lifecycle is owned by the caller (typically {@link
 * db.documenter.internal.builder.PostgresqlSchemaBuilder}).
 */
public final class PostgresqlQueryRunner extends AbstractQueryRunner {

  private static final Logger LOGGER = Logger.getLogger(PostgresqlQueryRunner.class.getName());

  private final PostgresqlPreparedStatements postgresqlQueries;
  private final PostgresqlPreparedStatementMapper postgresqlPreparedStatementMapper;
  private final PostgresqlResultSetMapper postgresqlResultSetMapper;

  private final Connection postgresqlConnection;

  /**
   * Constructs a {@code PostgresqlQueryRunner} with the required collaborators.
   *
   * @param postgresqlQueries the PostgreSQL SQL query string source
   * @param postgresqlPreparedStatementMapper the parameter binder for PostgreSQL prepared
   *     statements
   * @param postgresqlResultSetMapper the result set mapper for PostgreSQL metadata
   * @param connection the JDBC connection; lifecycle owned by the caller
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "Connection lifecycle managed by caller (SchemaBuilder) via try-with-resources. Field is final and private.")
  public PostgresqlQueryRunner(
      final PostgresqlPreparedStatements postgresqlQueries,
      final PostgresqlPreparedStatementMapper postgresqlPreparedStatementMapper,
      final PostgresqlResultSetMapper postgresqlResultSetMapper,
      final Connection connection) {
    super(
        postgresqlQueries,
        postgresqlPreparedStatementMapper,
        postgresqlResultSetMapper,
        connection);
    this.postgresqlQueries = postgresqlQueries;
    this.postgresqlPreparedStatementMapper = postgresqlPreparedStatementMapper;
    this.postgresqlResultSetMapper = postgresqlResultSetMapper;
    this.postgresqlConnection = connection;
  }

  /**
   * Retrieves all enum types defined in the specified schema.
   *
   * @param schema the schema to query for enum types
   * @return list of enums (name and schema only, values empty); never null, may be empty
   * @throws SQLException if the database query fails
   */
  public List<DbEnum> getEnumInfo(final String schema) throws SQLException {
    try (final var preparedStatement =
        postgresqlQueries.enumInfoPreparedStatement(postgresqlConnection)) {
      postgresqlPreparedStatementMapper.prepareEnumInfoStatement(preparedStatement, schema);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<DbEnum> dbEnums = postgresqlResultSetMapper.mapToDbEnumInfo(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} enums in schema: {1}",
              new Object[] {dbEnums.size(), LogUtils.sanitizeForLog(schema)});
        }

        return dbEnums;
      }
    }
  }

  /**
   * Retrieves the ordered values for a specific enum type.
   *
   * @param schema the schema containing the enum
   * @param dbEnum the enum whose values are to be retrieved
   * @return ordered list of enum label strings; never null, may be empty
   * @throws SQLException if the database query fails
   */
  public List<String> getEnumValues(final String schema, final DbEnum dbEnum) throws SQLException {
    try (final var preparedStatement =
        postgresqlQueries.enumValuesPreparedStatement(postgresqlConnection)) {
      postgresqlPreparedStatementMapper.prepareEnumValuesStatement(
          preparedStatement, schema, dbEnum.enumName());

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<String> dbEnumValues = postgresqlResultSetMapper.mapToDbEnumValues(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} values for enum {1} in schema: {2}",
              new Object[] {
                dbEnumValues.size(),
                LogUtils.sanitizeForLog(dbEnum.enumName()),
                LogUtils.sanitizeForLog(schema)
              });
        }

        return dbEnumValues;
      }
    }
  }

  /**
   * Retrieves the column-to-UDT mappings for all USER-DEFINED columns in the specified schema.
   *
   * @param schemaName the schema to query for UDT mappings
   * @return map from column key to UDT reference; never null, may be empty
   * @throws SQLException if the database query fails
   */
  public Map<ColumnKey, UdtReference> getColumnUdtMappings(final String schemaName)
      throws SQLException {
    try (final var preparedStatement =
        postgresqlQueries.columnUdtMappingsPreparedStatement(postgresqlConnection)) {
      postgresqlPreparedStatementMapper.prepareColumnUdtMappingsStatement(
          preparedStatement, schemaName);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final Map<ColumnKey, UdtReference> columnUdtMappings =
            postgresqlResultSetMapper.mapToColumnUdtMappings(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} column UDT mappings in schema: {1}",
              new Object[] {columnUdtMappings.size(), LogUtils.sanitizeForLog(schemaName)});
        }

        return columnUdtMappings;
      }
    }
  }

  /**
   * Retrieves all composite type definitions and their attributes for the specified schema.
   *
   * @param schema the schema to query for composite types
   * @return list of composite types with their fields; never null, may be empty
   * @throws SQLException if the database query fails
   */
  public List<DbCompositeType> getCompositeTypeInfo(final String schema) throws SQLException {
    try (final var preparedStatement =
        postgresqlQueries.compositeTypeInfoPreparedStatement(postgresqlConnection)) {
      postgresqlPreparedStatementMapper.prepareCompositeTypeInfoStatement(
          preparedStatement, schema);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<DbCompositeType> compositeTypes =
            postgresqlResultSetMapper.mapToDbCompositeTypeInfo(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} composite types in schema: {1}",
              new Object[] {compositeTypes.size(), LogUtils.sanitizeForLog(schema)});
        }

        return compositeTypes;
      }
    }
  }

  /**
   * Retrieves all materialized view names in the specified schema.
   *
   * @param schema the schema to query for materialized views
   * @return list of materialized views (name only, columns empty); never null, may be empty
   * @throws SQLException if the database query fails
   */
  public List<MaterializedView> getMaterializedViewInfo(final String schema) throws SQLException {
    try (final var preparedStatement =
        postgresqlQueries.materializedViewInfoPreparedStatement(postgresqlConnection)) {
      postgresqlPreparedStatementMapper.prepareMaterializedViewInfoStatement(
          preparedStatement, schema);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<MaterializedView> materializedViews =
            postgresqlResultSetMapper.mapToMaterializedViews(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} materialized views in schema: {1}",
              new Object[] {materializedViews.size(), LogUtils.sanitizeForLog(schema)});
        }

        return materializedViews;
      }
    }
  }

  /**
   * Retrieves all column definitions for a specific materialized view.
   *
   * @param schema the schema containing the materialized view
   * @param matViewName the name of the materialized view
   * @return list of columns; never null, may be empty
   * @throws SQLException if the database query fails
   */
  public List<Column> getMaterializedViewColumnInfo(final String schema, final String matViewName)
      throws SQLException {
    try (final var preparedStatement =
        postgresqlQueries.materializedViewColumnInfoPreparedStatement(postgresqlConnection)) {
      postgresqlPreparedStatementMapper.prepareMaterializedViewColumnInfoStatement(
          preparedStatement, schema, matViewName);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final List<Column> columns =
            postgresqlResultSetMapper.mapToMaterializedViewColumns(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} columns for materialized view: {1} in schema: {2}",
              new Object[] {
                columns.size(),
                LogUtils.sanitizeForLog(matViewName),
                LogUtils.sanitizeForLog(schema)
              });
        }
        return columns;
      }
    }
  }

  /**
   * Retrieves the child partition names for all partitioned tables in the specified schema.
   *
   * @param schema the schema to query for partition children
   * @return map from parent table name to ordered list of partition child names; never null, may be
   *     empty
   * @throws SQLException if the database query fails
   */
  public Map<String, List<String>> getPartitionChildren(final String schema) throws SQLException {
    try (final var preparedStatement =
        postgresqlQueries.partitionChildrenPreparedStatement(postgresqlConnection)) {
      postgresqlPreparedStatementMapper.preparePartitionChildrenStatement(
          preparedStatement, schema);

      try (final var resultSet = preparedStatement.executeQuery()) {
        final Map<String, List<String>> partitionChildren =
            postgresqlResultSetMapper.mapToPartitionChildren(resultSet);

        if (LOGGER.isLoggable(Level.INFO)) {
          LOGGER.log(
              Level.INFO,
              "Discovered: {0} partition entries in schema: {1}",
              new Object[] {partitionChildren.size(), LogUtils.sanitizeForLog(schema)});
        }

        return partitionChildren;
      }
    }
  }
}
