package dev.shanelucy.dbdocumenter.testhelpers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps a {@link Connection} and counts the statement executions issued through it, so tests can
 * assert that a reader issues a fixed number of queries however many schemas or tables it reads.
 *
 * <p>Every {@link Statement}, {@link PreparedStatement} and {@link CallableStatement} the wrapped
 * connection returns is wrapped too, and each call to {@code execute}, {@code executeQuery}, {@code
 * executeUpdate}, {@code executeLargeUpdate}, {@code executeBatch} or {@code executeLargeBatch}
 * counts one, whether or not it succeeds. A prepared statement executed three times counts three. A
 * wrapped statement's {@code getConnection()} returns the counting proxy. All other calls are
 * forwarded unchanged, and exceptions reach the caller unwrapped (the original {@code
 * SQLException}, not an {@link InvocationTargetException}).
 *
 * <p><b>Blind spot:</b> queries the driver issues internally, in particular those behind {@link
 * Connection#getMetaData()} and its {@link java.sql.DatabaseMetaData} methods, are <em>not</em>
 * counted. Neither are statements reached around the proxy, through {@code unwrap} or {@link
 * java.sql.ResultSet#getStatement()}.
 *
 * <p>The wrapped connection is still owned by the caller; closing the proxy closes it, as a direct
 * call would.
 *
 * <p><b>Thread Safety:</b> the counter is atomic, so wrapped statements may execute on several
 * threads; the underlying connection's own thread-safety rules still apply.
 */
public final class QueryCountingConnection {

  private static final Set<String> EXECUTE_METHODS =
      Set.of(
          "execute",
          "executeQuery",
          "executeUpdate",
          "executeLargeUpdate",
          "executeBatch",
          "executeLargeBatch");

  private final AtomicInteger executionCount = new AtomicInteger();
  private final Connection connection;

  private QueryCountingConnection(final Connection delegate) {
    this.connection = newProxy(Connection.class, new ConnectionHandler(delegate));
  }

  /**
   * Wraps a connection.
   *
   * @param delegate the connection to wrap; still owned by the caller
   * @return a counter whose {@link #connection()} forwards to {@code delegate}, starting at zero
   * @throws NullPointerException if {@code delegate} is null
   */
  public static QueryCountingConnection wrap(final Connection delegate) {
    return new QueryCountingConnection(Objects.requireNonNull(delegate, "delegate"));
  }

  /** Returns the counting proxy to hand to the code under test. */
  public Connection connection() {
    return connection;
  }

  /** Returns the number of {@code execute*} calls since wrapping or the last {@link #reset()}. */
  public int executionCount() {
    return executionCount.get();
  }

  /** Sets the count back to zero, for example after test setup has run its own statements. */
  public void reset() {
    executionCount.set(0);
  }

  // The interface's own loader always sees the interface; a context loader might not.
  @SuppressWarnings("PMD.UseProperClassLoader")
  private static <T> T newProxy(final Class<T> type, final InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }

  /**
   * Answers {@code equals}, {@code hashCode} and {@code toString} for a proxy, or returns null when
   * the method should be forwarded.
   *
   * <p>Forwarding {@code equals} would compare the delegate with the proxy and never be true, so
   * proxies use identity instead.
   */
  @SuppressWarnings("PMD.CompareObjectsWithEquals") // proxy identity is the intended equality
  private static Object objectMethod(
      final Object proxy, final Object delegate, final Method method, final Object... arguments) {
    return switch (method.getName()) {
      case "equals" ->
          method.getParameterCount() == 1 ? Boolean.valueOf(proxy == arguments[0]) : null;
      case "hashCode" ->
          method.getParameterCount() == 0 ? Integer.valueOf(System.identityHashCode(proxy)) : null;
      case "toString" ->
          method.getParameterCount() == 0 ? "QueryCountingConnection proxy for " + delegate : null;
      default -> null;
    };
  }

  private static Object forward(
      final Object delegate, final Method method, final Object... arguments) throws Throwable {
    try {
      return method.invoke(delegate, arguments);
    } catch (final InvocationTargetException e) {
      throw e.getCause();
    }
  }

  /** Forwards connection calls, wrapping every statement they return. */
  private final class ConnectionHandler implements InvocationHandler {
    private final Connection delegate;

    private ConnectionHandler(final Connection delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] arguments)
        throws Throwable {
      final Object objectResult = objectMethod(proxy, delegate, method, arguments);
      if (objectResult != null) {
        return objectResult;
      }
      final Object result = forward(delegate, method, arguments);
      final Class<?> returnType = method.getReturnType();
      if (result instanceof Statement statement && Statement.class.isAssignableFrom(returnType)) {
        return wrapStatement(returnType.asSubclass(Statement.class), statement);
      }
      return result;
    }

    private <T extends Statement> T wrapStatement(final Class<T> type, final Statement statement) {
      return newProxy(type, new StatementHandler(statement));
    }
  }

  /** Forwards statement calls, counting executions. */
  private final class StatementHandler implements InvocationHandler {
    private final Statement delegate;

    private StatementHandler(final Statement delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] arguments)
        throws Throwable {
      final Object objectResult = objectMethod(proxy, delegate, method, arguments);
      if (objectResult != null) {
        return objectResult;
      }
      if (EXECUTE_METHODS.contains(method.getName())) {
        executionCount.incrementAndGet();
      }
      final Object result = forward(delegate, method, arguments);
      if ("getConnection".equals(method.getName()) && method.getParameterCount() == 0) {
        // Forwarded first so a closed statement still throws.
        return connection;
      }
      return result;
    }
  }
}
