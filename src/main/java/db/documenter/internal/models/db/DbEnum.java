package db.documenter.internal.models.db;

import java.util.List;

public record DbEnum(String enumName, String columnName, List<String> enumValues) {

  public DbEnum {
    enumValues = enumValues == null ? List.of() : List.copyOf(enumValues);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String enumName;
    private String columnName;
    private List<String> enumValues;

    public Builder enumName(final String enumName) {
      this.enumName = enumName;
      return this;
    }

    public Builder columnName(final String columnName) {
      this.columnName = columnName;
      return this;
    }

    public Builder enumValues(final List<String> enumValues) {
      this.enumValues = List.copyOf(enumValues);
      return this;
    }

    public DbEnum build() {
      return new DbEnum(enumName, columnName, enumValues);
    }
  }
}
