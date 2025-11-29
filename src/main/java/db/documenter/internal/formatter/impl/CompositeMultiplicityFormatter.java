package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.MultiplicityFormatter;
import db.documenter.internal.models.db.ForeignKey;
import java.util.ArrayList;
import java.util.List;

public record CompositeMultiplicityFormatter(List<MultiplicityFormatter> formatters)
    implements MultiplicityFormatter {

  public CompositeMultiplicityFormatter {
    formatters = formatters == null ? List.of() : List.copyOf(formatters);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String format(final ForeignKey foreignKey, final String current) {
    String result = current;
    for (MultiplicityFormatter formatter : formatters) {
      result = formatter.format(foreignKey, result);
    }
    return result;
  }

  public static class Builder {
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
