package db.documenter.internal.queries;

import db.documenter.DbDocumenterConfig;
import db.documenter.internal.queries.api.QueryRunner;
import db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import db.documenter.internal.queries.impl.postgresql.preparedstatements.PostgresqlPreparedStatementMapper;
import db.documenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper;
import java.sql.Connection;

public final class QueryRunnerFactory {

  private final DbDocumenterConfig dbDocumenterConfig;

  public QueryRunnerFactory(final DbDocumenterConfig dbDocumenterConfig) {
    this.dbDocumenterConfig = dbDocumenterConfig;
  }

  public QueryRunner createQueryRunner(final Connection connection) {
    return switch (dbDocumenterConfig.rdmsType()) {
      case POSTGRESQL -> createPostgresqlQueryRunner(connection);
    };
  }

  private PostgresqlQueryRunner createPostgresqlQueryRunner(final Connection connection) {
    return new PostgresqlQueryRunner(
        new PostgresqlPreparedStatementMapper(), new PostgresqlResultSetMapper(), connection);
  }
}
