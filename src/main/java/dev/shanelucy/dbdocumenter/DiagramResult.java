package db.documenter;

import db.documenter.internal.validation.Validators;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The PlantUML diagrams generated for a set of database schemas.
 *
 * <p>Enums are kept out of the ERD so the relationship diagram stays readable; columns of an enum
 * type still show the enum type name. They are rendered in a separate enum diagram instead.
 *
 * <p><b>Immutability:</b> This record is immutable and thread-safe.
 *
 * <p><b>Validation:</b> Validated in the compact constructor; validation failures throw {@link
 * IllegalArgumentException}.
 *
 * @param erdDiagram entity-relationship diagram of the tables and their relationships; not blank
 * @param enumDiagram diagram of the enum types, grouped by schema; empty when none of the
 *     documented schemas defines an enum, otherwise non-blank
 */
public record DiagramResult(String erdDiagram, Optional<String> enumDiagram) {

  /**
   * Validates the diagrams.
   *
   * @throws IllegalArgumentException if the ERD is blank or a present enum diagram is blank
   */
  public DiagramResult {
    Validators.isNotBlank(erdDiagram, "erdDiagram");
    Validators.isNotNull(enumDiagram, "enumDiagram");
    enumDiagram.ifPresent(diagram -> Validators.isNotBlank(diagram, "enumDiagram"));
  }

  /** Creates a new builder for {@link DiagramResult}. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for constructing {@link DiagramResult} instances using a fluent API. */
  public static final class Builder {
    private @Nullable String erdDiagram;
    private @Nullable String enumDiagram;

    /** Returns this builder for method chaining. */
    public Builder erdDiagram(final String erdDiagram) {
      this.erdDiagram = erdDiagram;
      return this;
    }

    /** Returns this builder for method chaining; leave unset or pass {@code null} for no enums. */
    public Builder enumDiagram(final @Nullable String enumDiagram) {
      this.enumDiagram = enumDiagram;
      return this;
    }

    /**
     * Builds the {@link DiagramResult}; the ERD diagram must have been set.
     *
     * @throws IllegalArgumentException if the ERD diagram is unset or any diagram is blank
     */
    public DiagramResult build() {
      return new DiagramResult(
          Validators.isNotNull(erdDiagram, "erdDiagram"), Optional.ofNullable(enumDiagram));
    }
  }
}
