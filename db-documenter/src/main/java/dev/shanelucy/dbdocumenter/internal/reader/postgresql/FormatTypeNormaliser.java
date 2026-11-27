package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normalises {@code format_type} output for a {@code pg_catalog} type to what it is under default
 * session settings, so the dump does not depend on the session.
 *
 * <p>Two settings change {@code format_type} output for types on its generic path (names such as
 * {@code text}, {@code uuid} or {@code jsonb}; the SQL-standard spellings such as {@code integer},
 * {@code character varying(20)} or {@code timestamp with time zone} are fixed strings and never
 * change):
 *
 * <ul>
 *   <li>{@code search_path}: a same-named type that is found first shadows the {@code pg_catalog}
 *       type, which is then qualified, as in {@code pg_catalog.text}. That needs {@code pg_catalog}
 *       listed explicitly after another schema, or a domain in the session's temporary schema,
 *       which is searched before {@code pg_catalog} even with the default {@code search_path}.
 *   <li>{@code quote_all_identifiers}: every generic name is quoted, as in {@code "text"}, {@code
 *       "text"[]} or {@code "pg_catalog"."text"}.
 * </ul>
 *
 * <p><b>Rule:</b> remove a leading {@code pg_catalog.} or {@code "pg_catalog".}, then unquote a
 * leading quoted name if default quoting would not have quoted it: it matches {@code
 * [a-z_][a-z0-9_]*} and is not a non-unreserved SQL keyword. Whatever follows the name ({@code []},
 * a type modifier) is kept. The rule is idempotent on default output, so it is applied whatever the
 * session's settings are. It was checked against the output for every {@code pg_catalog} type on
 * PostgreSQL 15.20 and 18.7, with each setting on and off.
 */
final class FormatTypeNormaliser {

  private static final String QUOTE = "\"";
  private static final char QUOTE_CHARACTER = '"';
  private static final String ESCAPED_QUOTE = "\"\"";
  private static final String SYSTEM_SCHEMA_PREFIX = PgCatalogMappings.SYSTEM_SCHEMA + ".";
  private static final String QUOTED_SYSTEM_SCHEMA_PREFIX =
      QUOTE + PgCatalogMappings.SYSTEM_SCHEMA + QUOTE + ".";

  /** The names {@code quote_identifier} leaves unquoted, apart from keywords. */
  private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]*");

  /**
   * The {@code pg_catalog} type names that are reserved, column-name or type-function-name keywords
   * on PostgreSQL 15 to 18 ({@code pg_get_keywords()} with {@code catcode <> 'U'}), which default
   * quoting keeps quoted, such as {@code "char"} and {@code "any"}.
   *
   * <p>{@code json} is deliberately absent: it became a keyword in PostgreSQL 16, but from then on
   * {@code format_type} prints it as a fixed SQL-standard spelling, while on 15 it is an ordinary
   * generic name, so a quoted {@code "json"} only ever comes from {@code quote_all_identifiers}.
   */
  private static final Set<String> KEYWORD_TYPE_NAMES =
      Set.of("any", "bit", "char", "interval", "numeric", "time", "timestamp", "varchar");

  private FormatTypeNormaliser() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Returns {@code formattedName} as {@code format_type} prints it under default settings.
   *
   * @param formattedName {@code format_type} output for a {@code pg_catalog} type
   * @return the normalised spelling, such as {@code text[]} for {@code "pg_catalog"."text"[]}
   */
  static String normalise(final String formattedName) {
    final String unqualified = withoutSystemSchema(formattedName);
    if (!unqualified.startsWith(QUOTE)) {
      return unqualified;
    }
    final int closingQuote = closingQuoteIndex(unqualified);
    if (closingQuote < 0) {
      return unqualified;
    }
    final String name = unqualified.substring(1, closingQuote).replace(ESCAPED_QUOTE, QUOTE);
    return isSafeUnquoted(name) ? name + unqualified.substring(closingQuote + 1) : unqualified;
  }

  private static String withoutSystemSchema(final String formattedName) {
    if (formattedName.startsWith(SYSTEM_SCHEMA_PREFIX)) {
      return formattedName.substring(SYSTEM_SCHEMA_PREFIX.length());
    }
    if (formattedName.startsWith(QUOTED_SYSTEM_SCHEMA_PREFIX)) {
      return formattedName.substring(QUOTED_SYSTEM_SCHEMA_PREFIX.length());
    }
    return formattedName;
  }

  /**
   * Returns the index of the quote closing the quoted identifier that starts at index 0, skipping
   * doubled quotes, or -1 if it is unterminated.
   */
  private static int closingQuoteIndex(final String quoted) {
    int index = 1;
    while (index < quoted.length()) {
      if (quoted.charAt(index) != QUOTE_CHARACTER) {
        index++;
      } else if (quoted.startsWith(ESCAPED_QUOTE, index)) {
        index += 2;
      } else {
        return index;
      }
    }
    return -1;
  }

  private static boolean isSafeUnquoted(final String name) {
    return SAFE_IDENTIFIER.matcher(name).matches() && !KEYWORD_TYPE_NAMES.contains(name);
  }
}
