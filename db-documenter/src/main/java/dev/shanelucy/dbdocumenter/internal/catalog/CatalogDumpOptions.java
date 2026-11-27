package dev.shanelucy.dbdocumenter.internal.catalog;

import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import org.jspecify.annotations.Nullable;

/**
 * Options for {@link CatalogDumpWriter}.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * @param mode how faithfully the dump reproduces the snapshot
 */
public record CatalogDumpOptions(Mode mode) {

  /** Validates the components. */
  public CatalogDumpOptions {
    Validators.isNotNull(mode, "mode");
  }

  /** Returns options for a {@link Mode#VERBATIM} dump. */
  public static CatalogDumpOptions verbatim() {
    return new CatalogDumpOptions(Mode.VERBATIM);
  }

  /** Creates a new builder for {@link CatalogDumpOptions}. */
  public static Builder builder() {
    return new Builder();
  }

  /** What the dump keeps from the snapshot. */
  public enum Mode {
    /**
     * Every fact as read, including constraint names, vendor type text and enum types; the format
     * of {@code docs/catalog-dump-format.md}.
     */
    VERBATIM
  }

  /**
   * Builder for constructing {@link CatalogDumpOptions} instances using a fluent API.
   *
   * <p>The mode must be set before calling {@link #build()}.
   */
  public static final class Builder {
    private @Nullable Mode mode;

    /** Returns this builder for method chaining. */
    public Builder mode(final Mode mode) {
      this.mode = mode;
      return this;
    }

    /** Builds the {@link CatalogDumpOptions}; validation happens in the compact constructor. */
    public CatalogDumpOptions build() {
      return new CatalogDumpOptions(Validators.isNotNull(mode, "mode"));
    }
  }
}
