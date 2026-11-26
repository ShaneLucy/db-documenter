package db.documenter.internal.formatter.impl.multiplicity;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.validation.Validators;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CompositeMultiplicityFormatter(List<MultiplicityFormatter> formatters)
    implements MultiplicityFormatter {

  public CompositeMultiplicityFormatter {
    formatters = formatters == null ? List.of() : List.copyOf(formatters);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String format(
      final ForeignKey foreignKey, final String currentSchemaName, final @Nullable String current) {
    @Nullable String result = current;
    for (MultiplicityFormatter formatter : formatters) {
      result = formatter.format(foreignKey, currentSchemaName, result);
    }
    return Validators.isNotNull(result, "formattedLine");
  }

  public static final class Builder {
    private final List<MultiplicityFormatter> formatters = new ArrayList<>();

    public Builder addFormatter(final MultiplicityFormatter formatter) {
      formatters.add(formatter);
      return this;
    }

    public CompositeMultiplicityFormatter build() {
      return new CompositeMultiplicityFormatter(formatters);
    }
  }
}
