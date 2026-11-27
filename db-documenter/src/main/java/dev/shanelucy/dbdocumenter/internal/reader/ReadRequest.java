package dev.shanelucy.dbdocumenter.internal.reader;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * What a {@link MetadataReader} should read.
 *
 * <p><b>Validation:</b> at least one schema, with no null, empty or duplicate names.
 * Whitespace-only names are allowed, because they are legal identifiers. Validation failures throw
 * {@link dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException}.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param schemas the schema names exactly as stored in the catalog (no case folding); order is
 *     preserved but does not affect the snapshot, which sorts schemas by name; defensively copied
 */
public record ReadRequest(List<String> schemas) {

  /** Validates and copies the schema names. */
  public ReadRequest {
    Validators.containsAtLeast1Item(schemas, "schemas");
    Validators.containsNoNullElements(schemas, "schemas");
    schemas.forEach(schema -> Validators.isNotEmpty(schema, "schemas element"));
    Validators.containsNoDuplicates(schemas, "schemas");
    schemas = List.copyOf(schemas);
  }

  /** Creates a new builder for {@link ReadRequest}. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link ReadRequest} instances using a fluent API.
   *
   * <p>The schemas must be set before calling {@link #build()}.
   */
  public static final class Builder {
    private @Nullable List<String> schemas;

    /** Returns this builder for method chaining. */
    public Builder schemas(final List<String> schemas) {
      this.schemas = schemas;
      return this;
    }

    /** Builds the {@link ReadRequest}; validation happens in the compact constructor. */
    public ReadRequest build() {
      return new ReadRequest(Validators.isNotNull(schemas, "schemas"));
    }
  }
}
