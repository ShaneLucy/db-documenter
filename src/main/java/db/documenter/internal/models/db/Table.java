package db.documenter.internal.models.db;

import java.util.List;

public record Table(
    String schema,
    String name,
    String type,
    List<Column> columns,
    PrimaryKey primaryKey,
    List<ForeignKey> foreignKeys) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String schema;
    private String name;
    private String type;
    private List<Column> columns;
    private PrimaryKey primaryKey;
    private List<ForeignKey> foreignKeys;

    public Builder schema(String schema) {
      this.schema = schema;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder columns(List<Column> columns) {
      this.columns = columns;
      return this;
    }

    public Builder primaryKey(final PrimaryKey primaryKey) {
      this.primaryKey = primaryKey;
      return this;
    }

    public Builder foreignKeys(List<ForeignKey> foreignKeys) {
      this.foreignKeys = foreignKeys;
      return this;
    }

    public Table build() {
      return new Table(schema, name, type, columns, primaryKey, foreignKeys);
    }
  }
}
