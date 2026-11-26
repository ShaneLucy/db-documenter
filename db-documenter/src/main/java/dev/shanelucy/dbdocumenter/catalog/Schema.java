package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A schema and the tables and enum types it contains.
 *
 * <p><b>Ordering:</b> tables and enum types are sorted by name in Java {@link String} order.
 *
 * <p><b>Validation:</b> every table and enum type belongs to this schema (its {@link
 * QualifiedName#schema()} equals {@code name}), and names are unique among tables and among enum
 * types.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param name the schema name as stored in the catalog; not empty
 * @param tables the tables in this schema; defensively copied and sorted
 * @param enumTypes the enum types in this schema; defensively copied and sorted
 */
public record Schema(String name, List<Table> tables, List<EnumType> enumTypes) {

  /**
   * Validates, copies and sorts the components.
   *
   * @throws IllegalArgumentException if a component is null or contains a null element, the name is
   *     empty, a table or enum type belongs to another schema, or two tables or two enum types
   *     share a name
   */
  public Schema {
    Validators.isNotEmpty(name, "name");
    Validators.containsNoNullElements(tables, "tables");
    Validators.containsNoNullElements(enumTypes, "enumTypes");
    validateContainment(name, tables.stream().map(Table::name).toList(), "tables");
    validateContainment(name, enumTypes.stream().map(EnumType::name).toList(), "enumTypes");
    CatalogValidation.validateUniqueKeys(tables, Table::name, "tables.name");
    CatalogValidation.validateUniqueKeys(enumTypes, EnumType::name, "enumTypes.name");
    tables = List.copyOf(CatalogOrder.sortedCopy(tables, Comparator.comparing(Table::name)));
    enumTypes =
        List.copyOf(CatalogOrder.sortedCopy(enumTypes, Comparator.comparing(EnumType::name)));
  }

  /** Creates a new builder for {@link Schema}. */
  public static Builder builder() {
    return new Builder();
  }

  private static void validateContainment(
      final String schemaName, final List<QualifiedName> names, final String propertyName) {
    for (final QualifiedName qualifiedName : names) {
      if (!qualifiedName.schema().equals(schemaName)) {
        throw new ValidationException(
            propertyName
                + " must all belong to schema "
                + schemaName
                + ", but contains "
                + qualifiedName);
      }
    }
  }

  /**
   * Builder for constructing {@link Schema} instances using a fluent API.
   *
   * <p>The name is required; the lists default to empty.
   */
  public static final class Builder {
    private @Nullable String name;
    private List<Table> tables = List.of();
    private List<EnumType> enumTypes = List.of();

    /** Returns this builder for method chaining. */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder tables(final List<Table> tables) {
      this.tables = tables;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder enumTypes(final List<EnumType> enumTypes) {
      this.enumTypes = enumTypes;
      return this;
    }

    /**
     * Builds the {@link Schema}; the name must have been set.
     *
     * @throws IllegalArgumentException if the name is unset or the schema is invalid
     */
    public Schema build() {
      return new Schema(Validators.isNotNull(name, "name"), tables, enumTypes);
    }
  }
}
