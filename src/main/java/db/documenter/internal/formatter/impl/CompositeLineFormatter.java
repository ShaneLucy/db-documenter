package db.documenter.internal.formatter.impl;

import db.documenter.internal.formatter.api.LineFormatter;
import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.Table;
import java.util.ArrayList;
import java.util.List;

public record CompositeLineFormatter(List<LineFormatter> lineFormatters) implements LineFormatter {

  public CompositeLineFormatter {
    lineFormatters = lineFormatters == null ? List.of() : List.copyOf(lineFormatters);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String format(final Table table, final Column column, final String current) {
    String result = current;
    for (LineFormatter lineFormatter : lineFormatters) {
      result = lineFormatter.format(table, column, result);
    }
    return result;
  }

  public static class Builder {
    private final List<LineFormatter> lineFormatters = new ArrayList<>();

    public Builder addFormatter(final LineFormatter lineFormatter) {
      lineFormatters.add(lineFormatter);
      return this;
    }

    public CompositeLineFormatter build() {
      return new CompositeLineFormatter(lineFormatters);
    }
  }
}
