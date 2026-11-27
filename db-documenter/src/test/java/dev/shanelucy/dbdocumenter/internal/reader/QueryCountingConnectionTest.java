package dev.shanelucy.dbdocumenter.internal.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.shanelucy.dbdocumenter.testhelpers.QueryCountingConnection;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for the test-support {@link QueryCountingConnection}.
 *
 * <p>Lives in a library package rather than {@code dev.shanelucy.dbdocumenter.testhelpers}: library
 * tests are patched into the {@code dev.shanelucy.dbdocumenter} module while test-support sits on
 * the classpath, so a test package of the same name would split the package and hide the class.
 */
class QueryCountingConnectionTest {

  private Connection delegate;
  private QueryCountingConnection counter;

  @BeforeEach
  void setUp() {
    delegate = mock(Connection.class);
    counter = QueryCountingConnection.wrap(delegate);
  }

  /** A statement call that may throw, so execute methods can be parameterized. */
  @FunctionalInterface
  interface StatementCall {
    void call(Statement statement) throws SQLException;
  }

  static Stream<Arguments> executeMethods() {
    return Stream.of(
        Arguments.of("execute", (StatementCall) statement -> statement.execute("SELECT 1")),
        Arguments.of(
            "executeQuery", (StatementCall) statement -> statement.executeQuery("SELECT 1")),
        Arguments.of(
            "executeUpdate", (StatementCall) statement -> statement.executeUpdate("DELETE FROM t")),
        Arguments.of(
            "executeLargeUpdate",
            (StatementCall) statement -> statement.executeLargeUpdate("DELETE FROM t")),
        Arguments.of("executeBatch", (StatementCall) Statement::executeBatch),
        Arguments.of("executeLargeBatch", (StatementCall) Statement::executeLargeBatch));
  }

  @Nested
  class CountingTests {

    @Test
    void itStartsAtZero() {
      assertEquals(0, counter.executionCount());
    }

    @Test
    void itCountsEveryExecutionOfOnePreparedStatement() throws SQLException {
      when(delegate.prepareStatement("SELECT 1")).thenReturn(mock(PreparedStatement.class));
      final var statement = counter.connection().prepareStatement("SELECT 1");

      statement.executeQuery();
      statement.executeQuery();
      statement.executeQuery();

      assertEquals(3, counter.executionCount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(
        "dev.shanelucy.dbdocumenter.internal.reader.QueryCountingConnectionTest#executeMethods")
    void itCountsEachExecuteMethod(final String methodName, final StatementCall call)
        throws SQLException {
      when(delegate.createStatement()).thenReturn(mock(Statement.class));
      final var statement = counter.connection().createStatement();

      call.call(statement);

      assertEquals(1, counter.executionCount(), methodName + " should count once");
    }

    @Test
    void itCountsAcrossStatements() throws SQLException {
      when(delegate.createStatement()).thenReturn(mock(Statement.class));
      when(delegate.prepareStatement("SELECT 1")).thenReturn(mock(PreparedStatement.class));

      counter.connection().createStatement().execute("SELECT 2");
      counter.connection().prepareStatement("SELECT 1").executeQuery();

      assertEquals(2, counter.executionCount());
    }

    @Test
    void itDoesNotCountParameterBindingOrClose() throws SQLException {
      final var delegateStatement = mock(PreparedStatement.class);
      when(delegate.prepareStatement("SELECT ?")).thenReturn(delegateStatement);
      final var statement = counter.connection().prepareStatement("SELECT ?");

      statement.setString(1, "app");
      statement.close();

      assertEquals(0, counter.executionCount());
      verify(delegateStatement).setString(1, "app");
      verify(delegateStatement).close();
    }

    @Test
    void itDoesNotCountMetaDataCalls() throws SQLException {
      final var metaData = mock(DatabaseMetaData.class);
      when(delegate.getMetaData()).thenReturn(metaData);

      final var result = counter.connection().getMetaData();
      result.getTables(null, "app", "%", null);

      assertSame(metaData, result);
      assertEquals(0, counter.executionCount());
    }

    @Test
    void itResetsCountToZero() throws SQLException {
      when(delegate.createStatement()).thenReturn(mock(Statement.class));
      final var statement = counter.connection().createStatement();
      statement.execute("SELECT 1");
      statement.execute("SELECT 1");

      counter.reset();
      statement.execute("SELECT 1");

      assertEquals(1, counter.executionCount());
    }

    @Test
    void itPropagatesSqlExceptionUnwrappedAndStillCounts() throws SQLException {
      final var failure = new SQLException("relation does not exist", "42P01");
      final var delegateStatement = mock(PreparedStatement.class);
      when(delegateStatement.executeQuery()).thenThrow(failure);
      when(delegate.prepareStatement("SELECT 1")).thenReturn(delegateStatement);
      final var statement = counter.connection().prepareStatement("SELECT 1");

      final var exception = assertThrows(SQLException.class, statement::executeQuery);

      assertSame(failure, exception);
      assertEquals(1, counter.executionCount());
    }
  }

  @Nested
  class WrappingTests {

    @Test
    void itWrapsPrepareCallResultAsCallableStatement() throws SQLException {
      final var delegateStatement = mock(CallableStatement.class);
      when(delegate.prepareCall("{call f()}")).thenReturn(delegateStatement);

      final var statement = counter.connection().prepareCall("{call f()}");
      statement.execute();

      assertInstanceOf(CallableStatement.class, statement);
      assertNotSame(delegateStatement, statement);
      assertEquals(1, counter.executionCount());
    }

    @Test
    void itReturnsProxyFromStatementGetConnection() throws SQLException {
      final var delegateStatement = mock(PreparedStatement.class);
      when(delegateStatement.getConnection()).thenReturn(delegate);
      when(delegate.prepareStatement("SELECT 1")).thenReturn(delegateStatement);

      final var statement = counter.connection().prepareStatement("SELECT 1");

      assertSame(counter.connection(), statement.getConnection());
    }

    @Test
    void itClosesDelegateWhenProxyIsClosed() throws SQLException {
      counter.connection().close();

      verify(delegate).close();
    }

    @Test
    void itThrowsExceptionWhenDelegateIsNull() {
      final var exception =
          assertThrows(NullPointerException.class, () -> QueryCountingConnection.wrap(null));

      assertEquals("delegate", exception.getMessage());
    }
  }
}
