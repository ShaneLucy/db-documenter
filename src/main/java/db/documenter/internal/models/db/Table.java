package db.documenter.internal.models.db;

import java.util.List;

public record Table(
    String schema,
    String name,
    String type,
    List<Column> columns,
    PrimaryKey primaryKey,
    List<ForeignKey> foreignKeys) {

  public Table {
    columns = columns == null ? List.of() : List.copyOf(columns);
    foreignKeys = foreignKeys == null ? List.of() : List.copyOf(foreignKeys);
  }

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

    public Builder schema(final String schema) {
      this.schema = schema;
      return this;
    }

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder type(final String type) {
      this.type = type;
      return this;
    }

    public Builder columns(final List<Column> columns) {
      this.columns = List.copyOf(columns);
      return this;
    }

    public Builder primaryKey(final PrimaryKey primaryKey) {
      this.primaryKey = primaryKey;
      return this;
    }

    public Builder foreignKeys(final List<ForeignKey> foreignKeys) {
      this.foreignKeys = List.copyOf(foreignKeys);
      return this;
    }

    public Table build() {
      return new Table(schema, name, type, columns, primaryKey, foreignKeys);
    }
  }

  public static Table combineTableColumnsPrimaryAndForeignKeys(
      final Table table,
      final List<Column> columns,
      final PrimaryKey primaryKey,
      final List<ForeignKey> foreignKeys) {
    return Table.builder()
        .schema(table.schema())
        .name(table.name())
        .type(table.type())
        .columns(columns)
        .primaryKey(primaryKey)
        .foreignKeys(foreignKeys)
        .build();
  }
}
