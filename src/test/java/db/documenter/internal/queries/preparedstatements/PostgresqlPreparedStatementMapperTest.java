package db.documenter.internal.queries.preparedstatements;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import db.documenter.internal.models.db.Table;
import db.documenter.internal.queries.impl.postgresql.preparedstatements.PostgresqlPreparedStatementMapper;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresqlPreparedStatementMapperTest {
  private PostgresqlPreparedStatementMapper mapper;

  @Mock private PreparedStatement preparedStatement;

  @BeforeEach
  void setUp() {
    Mockito.reset(preparedStatement);
    mapper = new PostgresqlPreparedStatementMapper();
  }

  @Nested
  class PrepareTableInfoStatementTests {

    @Test
    void itSetsSchemaParameterCorrectly() throws SQLException {
      final String schema = "public";
      mapper.prepareTableInfoStatement(preparedStatement, schema);

      verify(preparedStatement).setString(1, schema);
      verifyNoMoreInteractions(preparedStatement);
    }
  }

  @Nested
  class PrepareColumnInfoStatementTests {

    @Test
    void itSetsSchemaAndTableNameParametersCorrectly() throws SQLException {
      final String schema = "public";
      final String tableName = "my_table";
      mapper.prepareColumnInfoStatement(preparedStatement, schema, tableName);

      verify(preparedStatement).setString(1, schema);
      verify(preparedStatement).setString(2, tableName);
      verifyNoMoreInteractions(preparedStatement);
    }
  }

  @Nested
  class PreparePrimaryKeyInfoStatementTests {

    @Test
    void itSetsSchemaAndTableNameParametersCorrectly() throws SQLException {
      final String schema = "public";
      final Table table = Table.builder().name("my_table").build();
      mapper.preparePrimaryKeyInfoStatement(preparedStatement, schema, table);

      verify(preparedStatement).setString(1, schema);
      verify(preparedStatement).setString(2, table.name());
      verifyNoMoreInteractions(preparedStatement);
    }
  }

  @Nested
  class PrepareForeignKeyInfoStatementTests {

    @Test
    void itSetsSchemaAndTableNameParametersCorrectly() throws SQLException {
      final String schema = "public";
      final Table table = Table.builder().name("my_table").build();
      mapper.prepareForeignKeyInfoStatement(preparedStatement, schema, table);

      verify(preparedStatement).setString(1, schema);
      verify(preparedStatement).setString(2, table.name());
      verifyNoMoreInteractions(preparedStatement);
    }
  }
}
