package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A read-only snapshot of the metadata of one or more schemas in a database.
 *
 * <p>Only what was read is included: foreign keys and column types may refer to tables or types
 * outside the snapshot.
 *
 * <p><b>Ordering:</b> schemas are sorted by name in Java {@link String} order.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param productName the database product name as the driver reports it; not blank
 * @param productVersion the database product version as the driver reports it; not blank
 * @param schemas the schemas read, with unique names; defensively copied and sorted
 */
public record CatalogSnapshot(String productName, String productVersion, List<Schema> schemas) {

  /**
   * Validates, copies and sorts the components.
   *
   * @throws IllegalArgumentException if a product string is null or blank, the schemas are null or
   *     contain a null, or two schemas share a name
   */
  public CatalogSnapshot {
    Validators.isNotBlank(productName, "productName");
    Validators.isNotBlank(productVersion, "productVersion");
    Validators.containsNoNullElements(schemas, "schemas");
    CatalogValidation.validateUniqueKeys(schemas, Schema::name, "schemas.name");
    schemas = List.copyOf(CatalogOrder.sortedCopy(schemas, Comparator.comparing(Schema::name)));
  }

  /** Creates a new builder for {@link CatalogSnapshot}. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link CatalogSnapshot} instances using a fluent API.
   *
   * <p>The product name and version are required; the schemas default to empty.
   */
  public static final class Builder {
    private @Nullable String productName;
    private @Nullable String productVersion;
    private List<Schema> schemas = List.of();

    /** Returns this builder for method chaining. */
    public Builder productName(final String productName) {
      this.productName = productName;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder productVersion(final String productVersion) {
      this.productVersion = productVersion;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder schemas(final List<Schema> schemas) {
      this.schemas = schemas;
      return this;
    }

    /**
     * Builds the {@link CatalogSnapshot}; the product name and version must have been set.
     *
     * @throws IllegalArgumentException if a product string is unset or blank, or the snapshot is
     *     invalid
     */
    public CatalogSnapshot build() {
      return new CatalogSnapshot(
          Validators.isNotNull(productName, "productName"),
          Validators.isNotNull(productVersion, "productVersion"),
          schemas);
    }
  }
}
