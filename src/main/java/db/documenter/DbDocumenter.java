package db.documenter;

import db.documenter.internal.connection.ConnectionManagerFactory;
import db.documenter.internal.connection.api.ConnectionManager;
import db.documenter.internal.formatter.impl.multiplicity.CardinalityFormatter;
import db.documenter.internal.formatter.impl.entity.CompositeEntityLineFormatter;
import db.documenter.internal.formatter.impl.multiplicity.CompositeMultiplicityFormatter;
import db.documenter.internal.formatter.impl.entity.DefaultEntityLineFormatter;
import db.documenter.internal.formatter.impl.multiplicity.DefaultMultiplicityFormatter;
import db.documenter.internal.formatter.impl.entity.ForeignKeyEntityLineFormatter;
import db.documenter.internal.formatter.impl.entity.NullableEntityLineFormatter;
import db.documenter.internal.formatter.impl.entity.PrimaryKeyEntityLineFormatter;
import db.documenter.internal.models.db.*;
import db.documenter.internal.queries.QueryRunnerFactory;
import db.documenter.internal.renderer.impl.EntityRenderer;
import db.documenter.internal.renderer.impl.EnumRenderer;
import db.documenter.internal.renderer.impl.RelationshipRenderer;
import db.documenter.internal.renderer.impl.SchemaRenderer;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Entrypoint to the DbDocumenter application. */
public final class DbDocumenter {
  private final ConnectionManager connectionManager;
  private final DbDocumenterConfig dbDocumenterConfig;
  private final QueryRunnerFactory queryRunnerFactory;

  public DbDocumenter(final DbDocumenterConfig dbDocumenterConfig) {
    this.connectionManager =
        new ConnectionManagerFactory(dbDocumenterConfig).createConnectionManager();
    this.queryRunnerFactory = new QueryRunnerFactory(dbDocumenterConfig);
    this.dbDocumenterConfig = dbDocumenterConfig;
  }

  /**
   * Inspects the provided database schema(s) to generate a puml file.
   *
   * @return a formatted {@link String} representation of the database schema.
   */
  public String generatePuml() {
    final var formatter =
        CompositeEntityLineFormatter.builder()
            .addFormatter(new DefaultEntityLineFormatter())
            .addFormatter(new PrimaryKeyEntityLineFormatter())
            .addFormatter(new ForeignKeyEntityLineFormatter())
            .addFormatter(new NullableEntityLineFormatter())
            .build();
    final var multiplicityFormatter =
        CompositeMultiplicityFormatter.builder()
            .addFormatter(new DefaultMultiplicityFormatter())
            .addFormatter(new CardinalityFormatter())
            .build();
    final var entityRenderer = new EntityRenderer(formatter);
    final var relationShipRenderer = new RelationshipRenderer(multiplicityFormatter);
    final var enumRenderer = new EnumRenderer();
    final var schemaRenderer =
        new SchemaRenderer(entityRenderer, relationShipRenderer, enumRenderer);
    final List<Schema> schemas = buildSchemas();

    return schemaRenderer.render(schemas);
  }

  private List<Schema> buildSchemas() {
    return dbDocumenterConfig.schemas().stream()
        .map(
            schema -> {
              try {
                final List<DbEnum> dbEnums = buildDbEnums(schema);
                return Schema.builder()
                    .name(schema)
                    .tables(buildTables(schema, dbEnums))
                    .dbEnums(dbEnums)
                    .build();
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }

  private List<Table> buildTables(final String schema, final List<DbEnum> dbEnums)
      throws SQLException {
    try (final var connection = connectionManager.getConnection()) {
      final var queryRunner = queryRunnerFactory.createQueryRunner(connection);
      final var tables = queryRunner.getTableInfo(schema);

      return tables.stream()
          .map(
              table -> {
                try {
                  final List<Column> columns =
                      queryRunner.getColumnInfo(schema, table).stream()
                          .map(
                              column -> {
                                if (!Objects.equals(column.dataType(), "USER-DEFINED")) {
                                  return column;
                                }

                                return Column.mapUserDefinedToEnumType(column, dbEnums);
                              })
                          .toList();

                  final var primaryKey = queryRunner.getPrimaryKeyInfo(schema, table);

                  final List<ForeignKey> foreignKeys =
                      queryRunner.getForeignKeyInfo(schema, table).stream()
                          .map(
                              foreignKey -> {
                                final var optionalColumn =
                                    columns.stream()
                                        .filter(
                                            column ->
                                                column.name().equals(foreignKey.sourceColumn()))
                                        .findFirst();
                                if (optionalColumn.isEmpty()) {
                                  return foreignKey;
                                }

                                return ForeignKey.combineForeignKeyAndIsNullable(
                                    foreignKey, optionalColumn.get().isNullable());
                              })
                          .toList();

                  return Table.combineTableColumnsPrimaryAndForeignKeys(
                      table, columns, primaryKey, foreignKeys);
                } catch (SQLException e) {
                  throw new RuntimeException(e);
                }
              })
          .toList();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private List<DbEnum> buildDbEnums(final String schema) throws SQLException {
    try (final var connection = connectionManager.getConnection()) {
      final var queryRunner = queryRunnerFactory.createQueryRunner(connection);

      final List<DbEnum> dbEnums = queryRunner.getEnumInfo(schema);

      return dbEnums.stream()
          .map(
              dbEnum -> {
                try {
                  final List<String> dbEnumValues = queryRunner.getEnumValues(schema, dbEnum);
                  return DbEnum.combineDbEnumValuesAndInfo(dbEnum, dbEnumValues);
                } catch (SQLException e) {
                  throw new RuntimeException(e);
                }
              })
          .toList();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
