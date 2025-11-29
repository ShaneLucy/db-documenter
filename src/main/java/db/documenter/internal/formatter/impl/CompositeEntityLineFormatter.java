package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.EntityLineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;
import java.util.ArrayList;
import java.util.List;

public record CompositeEntityLineFormatter(List<EntityLineFormatter> lineFormatters)
    implements EntityLineFormatter {

  public CompositeEntityLineFormatter {
    lineFormatters = lineFormatters == null ? List.of() : List.copyOf(lineFormatters);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String format(final Table table, final Column column, final String current) {
    String result = current;
    for (EntityLineFormatter lineFormatter : lineFormatters) {
      result = lineFormatter.format(table, column, result);
    }
    return result;
  }

  public static class Builder {
    private final List<EntityLineFormatter> lineFormatters = new ArrayList<>();

    public Builder addFormatter(final EntityLineFormatter lineFormatter) {
      lineFormatters.add(lineFormatter);
      return this;
    }

    public CompositeEntityLineFormatter build() {
      return new CompositeEntityLineFormatter(lineFormatters);
    }
  }
}
