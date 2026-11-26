package dev.shanelucy.dbdocumenter.catalog;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The data type of a column.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param displayText the type as the database presents it, such as {@code varchar(20)} or {@code
 *     int4[]}; never null, but may be empty where the vendor allows an undeclared type
 * @param userType the user-defined type (enum, domain, composite, range) the column uses, or for an
 *     array of one, its element type; empty for built-in types. The type may lie outside the
 *     snapshot.
 */
public record DataType(String displayText, Optional<QualifiedName> userType) {

  /**
   * Validates the components.
   *
   * @throws IllegalArgumentException if either component is null
   */
  public DataType {
    Validators.isNotNull(displayText, "displayText");
    Validators.isNotNull(userType, "userType");
  }

  /** Creates a new builder for {@link DataType}. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for constructing {@link DataType} instances using a fluent API. */
  public static final class Builder {
    private @Nullable String displayText;
    private @Nullable QualifiedName userType;

    /** Returns this builder for method chaining. */
    public Builder displayText(final String displayText) {
      this.displayText = displayText;
      return this;
    }

    /** Returns this builder for method chaining; leave unset for a built-in type. */
    public Builder userType(final @Nullable QualifiedName userType) {
      this.userType = userType;
      return this;
    }

    /**
     * Builds the {@link DataType}; the display text must have been set.
     *
     * @throws IllegalArgumentException if the display text is unset
     */
    public DataType build() {
      return new DataType(
          Validators.isNotNull(displayText, "displayText"), Optional.ofNullable(userType));
    }
  }
}
