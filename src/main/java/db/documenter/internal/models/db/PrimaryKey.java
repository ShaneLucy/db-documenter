package db.documenter.internal.models.db;

import java.util.List;

public record PrimaryKey(String constraintName, List<String> columnNames) {
  public static Builder builder() {
    return new Builder();
  }

  public PrimaryKey {
      columnNames = columnNames == null ? List.of() : List.copyOf(columnNames);
  }
  public static class Builder {
    private String constraintName;
    private List<String> columnNames;

    public Builder constraintName(final String constraintName) {
      this.constraintName = constraintName;
      return this;
    }

    public Builder columnNames(final List<String> columnNames) {
      this.columnNames = List.copyOf(columnNames);
      return this;
    }

    public PrimaryKey build() {
      return new PrimaryKey(constraintName, columnNames);
    }
  }
}
