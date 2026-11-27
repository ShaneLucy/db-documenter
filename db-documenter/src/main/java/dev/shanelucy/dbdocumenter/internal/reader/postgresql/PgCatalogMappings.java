package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import dev.shanelucy.dbdocumenter.catalog.DataType;
import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.catalog.ReferentialAction;
import dev.shanelucy.dbdocumenter.internal.reader.postgresql.PgCatalogRows.TypeRow;
import org.jspecify.annotations.Nullable;

/**
 * Pure mappings from PostgreSQL catalog codes and type facts to the catalog model.
 *
 * <p>Kept free of JDBC so the rules can be unit-tested without a database.
 */
final class PgCatalogMappings {

  /** The schema whose types are spelled with {@code format_type}. */
  static final String SYSTEM_SCHEMA = "pg_catalog";

  private PgCatalogMappings() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Maps a {@code pg_constraint.confdeltype} or {@code confupdtype} code to a referential action.
   *
   * @param code one of {@code a} (no action), {@code r} (restrict), {@code c} (cascade), {@code n}
   *     (set null) or {@code d} (set default)
   * @return the matching action
   * @throws IllegalArgumentException if the code is not one of the five PostgreSQL codes, which
   *     would mean a server version this reader does not understand
   */
  static ReferentialAction referentialAction(final char code) {
    return switch (code) {
      case 'a' -> ReferentialAction.NO_ACTION;
      case 'r' -> ReferentialAction.RESTRICT;
      case 'c' -> ReferentialAction.CASCADE;
      case 'n' -> ReferentialAction.SET_NULL;
      case 'd' -> ReferentialAction.SET_DEFAULT;
      default ->
          throw new IllegalArgumentException(
              "Unknown PostgreSQL referential action code '" + code + "'");
    };
  }

  /**
   * Maps a column's type facts to a {@link DataType} (plan decision B1).
   *
   * <ul>
   *   <li>A {@code pg_catalog} type (including arrays of one) uses {@code format_type}, such as
   *       {@code character varying(20)[]} or {@code "char"}, with no user type. The output is
   *       normalised by {@link FormatTypeNormaliser}, so {@code search_path} and {@code
   *       quote_all_identifiers} do not change it.
   *   <li>An array of any other type uses the element's bare {@code typname} plus {@code []}, and
   *       the element as its user type. Multi-dimensional arrays look the same, because PostgreSQL
   *       does not enforce dimensions.
   *   <li>Any other type (enum, domain, composite, range) uses its bare {@code typname} and itself
   *       as its user type; for a domain that is the domain, not its base type.
   * </ul>
   *
   * <p>User type names come from the raw catalog columns, never from {@code format_type} or a
   * {@code reg*} cast, because those depend on {@code search_path} and quote identifiers
   * themselves.
   */
  static DataType dataType(final TypeRow type) {
    if (SYSTEM_SCHEMA.equals(type.schema())) {
      return DataType.builder()
          .displayText(FormatTypeNormaliser.normalise(type.formattedName()))
          .build();
    }
    final @Nullable String elementSchema = type.elementSchema();
    final @Nullable String elementName = type.elementName();
    if (elementSchema != null && elementName != null) {
      return DataType.builder()
          .displayText(elementName + "[]")
          .userType(QualifiedName.of(elementSchema, elementName))
          .build();
    }
    return DataType.builder()
        .displayText(type.name())
        .userType(QualifiedName.of(type.schema(), type.name()))
        .build();
  }
}
