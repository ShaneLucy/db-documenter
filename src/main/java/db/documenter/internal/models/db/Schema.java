package db.documenter.internal.models.db;

import java.util.List;

public record Schema(String name, List<Table> tables) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private List<Table> tables;

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder tables(final List<Table> tables) {
      this.tables = tables;
      return this;
    }

    public Schema build() {
      return new Schema(name, tables);
    }
  }
}
