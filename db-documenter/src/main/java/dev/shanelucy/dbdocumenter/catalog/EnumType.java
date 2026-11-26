package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A user-defined enum type.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param name the schema-qualified type name
 * @param values the labels in declared order, which is the enum's sort order and is never changed;
 *     no nulls, no duplicates, may be empty; defensively copied
 */
public record EnumType(QualifiedName name, List<String> values) {

  /**
   * Validates and copies the components.
   *
   * @throws IllegalArgumentException if a component is null, or the values contain a null or a
   *     duplicate
   */
  public EnumType {
    Validators.isNotNull(name, "name");
    Validators.containsNoNullElements(values, "values");
    Validators.containsNoDuplicates(values, "values");
    values = List.copyOf(values);
  }

  /** Creates a new builder for {@link EnumType}. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for constructing {@link EnumType} instances using a fluent API.
   *
   * <p>The name is required; the values default to empty.
   */
  public static final class Builder {
    private @Nullable QualifiedName name;
    private List<String> values = List.of();

    /** Returns this builder for method chaining. */
    public Builder name(final QualifiedName name) {
      this.name = name;
      return this;
    }

    /** Returns this builder for method chaining. */
    public Builder values(final List<String> values) {
      this.values = values;
      return this;
    }

    /**
     * Builds the {@link EnumType}; the name must have been set.
     *
     * @throws IllegalArgumentException if the name is unset or the enum is invalid
     */
    public EnumType build() {
      return new EnumType(Validators.isNotNull(name, "name"), values);
    }
  }
}
