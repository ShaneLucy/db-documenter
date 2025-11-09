package db.documenter;

import static db.documenter.internal.queries.resultsets.ResultSetMapper.*;

import db.documenter.internal.db.api.ConnectionManager;
import db.documenter.internal.db.impl.PostgresConnectionManager;
import db.documenter.internal.formatter.impl.*;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.Schema;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.QueryRunner;
import db.documenter.internal.queries.preparedstatements.PreparedStatementMapper;
import db.documenter.internal.queries.resultsets.ResultSetMapper;
import db.documenter.internal.renderer.impl.EntityRenderer;
import db.documenter.internal.renderer.impl.RelationshipRenderer;
import db.documenter.internal.renderer.impl.SchemaRenderer;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class DbDocumenter {

  private final ConnectionManager connectionManager;
  private final DbDocumenterConfig dbDocumenterConfig;

  public DbDocumenter(final DbDocumenterConfig dbDocumenterConfig) {
    this.connectionManager = new PostgresConnectionManager(dbDocumenterConfig);
    this.dbDocumenterConfig = dbDocumenterConfig;
  }

  public String generatePuml() {
    final var formatter =
        new CompositeLineFormatter(
            List.of(
                new DefaultLineFormatter(),
                new PrimaryKeyLineFormatter(),
                new ForeignKeyLineFormatter(),
                new NullableLineFormatter()));
    final var entityRenderer = new EntityRenderer(formatter);
    final var relationShipRenderer = new RelationshipRenderer();
    final var schemaRenderer = new SchemaRenderer(entityRenderer, relationShipRenderer);
    final List<Schema> schemas = buildSchemas();

    return schemaRenderer.render(schemas);
  }

  public void writePumlToFile(final String fileName, Charset charset)
      throws SQLException, IOException {
    final String puml = generatePuml();

    try (final var writer = new FileOutputStream(fileName)) {
      writer.write(puml.getBytes(charset));
    }
  }

  private List<Schema> buildSchemas() {
    return dbDocumenterConfig.schemas().stream()
        .map(
            schema -> {
              try {
                return Schema.builder().name(schema).tables(buildTables(schema)).build();
              } catch (SQLException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }

  private List<Table> buildTables(final String schema) throws SQLException {
    final var resultSetMapper = new ResultSetMapper();

      try {
          final var queryRunner =
                  new QueryRunner(new PreparedStatementMapper(), new ResultSetMapper(), connectionManager);
        final var tables = queryRunner.getTableInfo(schema);

        return tables.stream()
            .map(
                table -> {
                  try {
                    final List<Column> columns = queryRunner.getColumnInfo(schema, table);

                    final var primaryKey = queryRunner.getPrimaryKeyInfo(schema, table);

                    final List<ForeignKey> foreignKeys =
                        queryRunner.getForeignKeyInfo(schema, table);

                    return resultSetMapper.combineTableColumnsPrimaryAndForeignKeys(
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
}
