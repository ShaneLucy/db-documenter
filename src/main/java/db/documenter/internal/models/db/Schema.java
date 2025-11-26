package db.documenter.internal.models.db;

import java.util.List;

public record Schema(String name, List<Table> tables, List<DbEnum> dbEnums) {

  public static Builder builder() {
    return new Builder();
  }

  public Schema {
    tables = tables == null ? List.of() : List.copyOf(tables);
    dbEnums = dbEnums == null ? List.of() : List.copyOf(dbEnums);
  }

  public static class Builder {
    private String name;
    private List<Table> tables;
    private List<DbEnum> dbEnums;

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder tables(final List<Table> tables) {
      this.tables = List.copyOf(tables);
      return this;
    }

    public Builder dbEnums(final List<DbEnum> dbEnums) {
      this.dbEnums = List.copyOf(dbEnums);
      return this;
    }

    public Schema build() {
      return new Schema(name, tables, dbEnums);
    }
  }
}
