package db.documenter.internal.utils;

/**
 * Utility methods for safe logging.
 *
 * <p>This class provides methods to sanitize log messages and prevent log injection attacks such as
 * CRLF injection.
 */
public final class LogUtils {

  private LogUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Sanitizes a value for safe inclusion in log messages by removing carriage return and newline
   * characters.
   *
   * <p>This prevents CRLF injection attacks where an attacker could inject fake log entries by
   * including newline characters in user-controlled data.
   *
   * <p><b>Example:</b>
   *
   * <pre>{@code
   * String userInput = "schema\nFAKE: Admin logged in";
   * String safe = LogUtils.sanitizeForLog(userInput);
   * // Result: "schema FAKE: Admin logged in" (newline replaced with space)
   * }</pre>
   *
   * @param value the value to sanitize (will be converted to String via toString())
   * @return sanitized string with CR and LF characters removed/replaced
   */
  public static String sanitizeForLog(final Object value) {
    return value.toString().replace("\r", "").replace("\n", " ");
  }
}
