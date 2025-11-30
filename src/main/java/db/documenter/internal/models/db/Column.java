package db.documenter.internal.models.db;

import java.util.List;

// TODO remove ordinal position
public record Column(
    String name,
    int ordinalPosition,
    boolean isNullable,
    String dataType,
    int maximumLength,
    List<Constraint> constraints) {

  public Column {
    constraints = constraints == null ? List.of() : List.copyOf(constraints);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private int ordinalPosition;
    private boolean isNullable;
    private String dataType;
    private int maximumLength;
    private List<Constraint> constraints;

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder ordinalPosition(final int ordinalPosition) {
      this.ordinalPosition = ordinalPosition;
      return this;
    }

    public Builder isNullable(final boolean isNullable) {
      this.isNullable = isNullable;
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
      return new Column(name, ordinalPosition, isNullable, dataType, maximumLength, constraints);
    }
  }
}
