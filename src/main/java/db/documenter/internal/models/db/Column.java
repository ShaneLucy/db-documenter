package db.documenter.internal.models.db;

import java.util.List;

public record Column(
    String name, String dataType, int maximumLength, List<Constraint> constraints) {

  public Column {
    constraints = constraints == null ? List.of() : List.copyOf(constraints);
  }

  public boolean isNullable() {
    return constraints != null && constraints.contains(Constraint.NULLABLE);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String dataType;
    private int maximumLength;
    private List<Constraint> constraints;

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder dataType(final String dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder maximumLength(final int maximumLength) {
      this.maximumLength = maximumLength;
      return this;
    }

    public Builder constraints(final List<Constraint> constraints) {
      this.constraints = List.copyOf(constraints);
      return this;
    }

    public Column build() {
      return new Column(name, dataType, maximumLength, constraints);
    }
  }
}
