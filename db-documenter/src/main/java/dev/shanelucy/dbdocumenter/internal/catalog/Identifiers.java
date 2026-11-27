package dev.shanelucy.dbdocumenter.internal.catalog;

import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Formats catalog identifiers so that any name, however unusual, prints as one unambiguous token.
 *
 * <p>Identifiers matching {@code [a-z0-9_]+} print bare; everything else is double-quoted with
 * {@code \n}, {@code \r}, {@code \t}, {@code \\} and {@code \"} escaped and any other unprintable
 * character written as {@code \}{@code uXXXX}. Quoting keeps mixed case, spaces and dots visible,
 * so {@code "a.b".c} cannot be confused with {@code a."b.c"}; escaping keeps every token on one
 * line. This is the token format of the catalog dump, so changing it changes every golden file.
 *
 * <p><b>Thread Safety:</b> stateless and thread-safe.
 */
public final class Identifiers {

  private static final Pattern BARE_IDENTIFIER = Pattern.compile("[a-z0-9_]+");

  private Identifiers() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Formats an identifier, quoting it unless it matches {@code [a-z0-9_]+}.
   *
   * @param identifier the identifier as stored in the catalog; may be empty, which formats as
   *     {@code ""}
   * @return the bare or quoted identifier; never empty
   */
  public static String format(final String identifier) {
    if (BARE_IDENTIFIER.matcher(identifier).matches()) {
      return identifier;
    }
    return quote(identifier);
  }

  /**
   * Formats a qualified name as {@code schema.name}, each part formatted with {@link
   * #format(String)}.
   *
   * @return the formatted name, for example {@code app.users} or {@code main."Mixed Case"}
   */
  public static String format(final QualifiedName qualifiedName) {
    return format(qualifiedName.schema()) + '.' + format(qualifiedName.name());
  }

  /**
   * Always double-quotes a value, escaping it as described in the class documentation.
   *
   * <p>For tokens that are quoted whatever their content, such as enum labels and type display text
   * in the catalog dump.
   *
   * @return the quoted, escaped value; at least {@code ""}
   */
  public static String quote(final String value) {
    final var quoted = new StringBuilder(value.length() + 2).append('"');
    value.codePoints().forEach(codePoint -> appendEscaped(quoted, codePoint));
    return quoted.append('"').toString();
  }

  private static void appendEscaped(final StringBuilder target, final int codePoint) {
    switch (codePoint) {
      case '\n' -> target.append("\\n");
      case '\r' -> target.append("\\r");
      case '\t' -> target.append("\\t");
      case '\\' -> target.append("\\\\");
      case '"' -> target.append("\\\"");
      default -> {
        if (isPrintable(codePoint)) {
          target.appendCodePoint(codePoint);
        } else {
          for (final char codeUnit : Character.toChars(codePoint)) {
            target.append(String.format(Locale.ROOT, "\\u%04X", (int) codeUnit));
          }
        }
      }
    }
  }

  /**
   * Returns whether a code point can be printed as itself.
   *
   * <p>Controls, invisible format characters (such as zero-width spaces and bidi overrides, which
   * could disguise a name), line and paragraph separators, unassigned and private-use code points,
   * and unpaired surrogates are escaped. A non-printable supplementary code point is escaped as its
   * UTF-16 surrogate pair, so every escape is exactly four hex digits.
   */
  private static boolean isPrintable(final int codePoint) {
    if (Character.isISOControl(codePoint)) {
      return false;
    }
    return switch (Character.getType(codePoint)) {
      case Character.FORMAT,
          Character.UNASSIGNED,
          Character.LINE_SEPARATOR,
          Character.PARAGRAPH_SEPARATOR,
          Character.PRIVATE_USE,
          Character.SURROGATE ->
          false;
      default -> true;
    };
  }
}
