package db.documenter.internal.queries.impl.postgresql.preparedstatements;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
      final String tableName = "my_table";
      mapper.preparePrimaryKeyInfoStatement(preparedStatement, schema, tableName);

      verify(preparedStatement).setString(1, schema);
      verify(preparedStatement).setString(2, tableName);
      verifyNoMoreInteractions(preparedStatement);
    }
  }

  @Nested
  class PrepareForeignKeyInfoStatementTests {

    @Test
    void itSetsSchemaAndTableNameParametersCorrectly() throws SQLException {
      final String schema = "public";
      final String tableName = "my_table";
      mapper.prepareForeignKeyInfoStatement(preparedStatement, schema, tableName);

      verify(preparedStatement).setString(1, schema);
      verify(preparedStatement).setString(2, tableName);
      verifyNoMoreInteractions(preparedStatement);
    }
  }

  @Nested
  class PrepareEnumInfoStatementTests {

    @Test
    void itSetsSchemaParameterCorrectly() throws SQLException {
      final String schema = "public";
      mapper.prepareEnumInfoStatement(preparedStatement, schema);

      verify(preparedStatement).setString(1, schema);
      verifyNoMoreInteractions(preparedStatement);
    }
  }

  @Nested
  class PrepareEnumValuesStatementTests {

    @Test
    void itSetsSchemaAndEnumNameParametersCorrectly() throws SQLException {
      final String schema = "public";
      final String enumName = "some enum name";
      mapper.prepareEnumValuesStatement(preparedStatement, schema, enumName);

      verify(preparedStatement).setString(1, schema);
      verify(preparedStatement).setString(2, enumName);
      verifyNoMoreInteractions(preparedStatement);
    }
  }

  @Nested
  class PreparePartitionChildrenStatementTests {

    @Test
    void itSetsSchemaAsFirstParameter() throws SQLException {
      final String schema = "public";
      mapper.preparePartitionChildrenStatement(preparedStatement, schema);

      verify(preparedStatement).setString(1, schema);
      verifyNoMoreInteractions(preparedStatement);
    }
  }
}
