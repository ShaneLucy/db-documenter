package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import dev.shanelucy.dbdocumenter.catalog.QualifiedName;
import dev.shanelucy.dbdocumenter.internal.exceptions.ValidationException;
import dev.shanelucy.dbdocumenter.internal.validation.Validators;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The raw rows of one {@link PgCatalogReader} run, one list per query, before they are joined into
 * the catalog model by {@link PgCatalogAssembler}.
 *
 * <p>Rows refer to their table by {@code pg_class} OID. Because the queries run as separate
 * statements, rows may refer to tables or schemas missing from the other lists after concurrent
 * DDL; the assembler drops such rows.
 *
 * <p><b>Immutability:</b> This record and its nested row records are immutable and thread-safe. All
 * lists are defensively copied and validated as containing no nulls.
 *
 * @param requestedSchemas the schema names the caller asked for, used only to log a warning for
 *     each one that is absent from the snapshot
 * @param schemaNames the requested schemas that exist and that the role has {@code USAGE} on
 * @param schemasWithoutUsage the requested schemas that exist but that the role has no {@code
 *     USAGE} on; they are absent from the snapshot
 * @param tables the visible tables in those schemas
 * @param columns the columns of the visible tables, in {@code attnum} order within each table
 * @param constraints the primary-key, unique and foreign-key constraints of the visible tables
 * @param uniqueIndexes the unique indexes that do not back a constraint and qualify as unique keys
 * @param enums the enum types in the requested schemas
 */
record PgCatalogRows(
    List<String> requestedSchemas,
    List<String> schemaNames,
    List<String> schemasWithoutUsage,
    List<TableRow> tables,
    List<ColumnRow> columns,
    List<ConstraintRow> constraints,
    List<UniqueIndexRow> uniqueIndexes,
    List<EnumRow> enums) {

  /** Validates and copies the lists. */
  PgCatalogRows {
    requestedSchemas =
        List.copyOf(Validators.containsNoNullElements(requestedSchemas, "requestedSchemas"));
    schemaNames = List.copyOf(Validators.containsNoNullElements(schemaNames, "schemaNames"));
    schemasWithoutUsage =
        List.copyOf(Validators.containsNoNullElements(schemasWithoutUsage, "schemasWithoutUsage"));
    tables = List.copyOf(Validators.containsNoNullElements(tables, "tables"));
    columns = List.copyOf(Validators.containsNoNullElements(columns, "columns"));
    constraints = List.copyOf(Validators.containsNoNullElements(constraints, "constraints"));
    uniqueIndexes = List.copyOf(Validators.containsNoNullElements(uniqueIndexes, "uniqueIndexes"));
    enums = List.copyOf(Validators.containsNoNullElements(enums, "enums"));
  }

  /** Creates a new builder for {@link PgCatalogRows}. */
  static Builder builder() {
    return new Builder();
  }

  /**
   * A table ({@code relkind} {@code r} or {@code p}, not a partition).
   *
   * @param oid the table's {@code pg_class} OID, which the other rows refer to
   * @param schema the schema name
   * @param name the table name
   * @param columnCount the number of live columns ({@code attnum > 0}, not dropped) when the tables
   *     query ran; the assembler drops the table if its column rows disagree, which means the table
   *     was dropped or altered between the queries
   */
  record TableRow(long oid, String schema, String name, int columnCount) {

    /** Validates the components. */
    TableRow {
      Validators.isNotEmpty(schema, "schema");
      Validators.isNotEmpty(name, "name");
      if (columnCount < 0) {
        throw new ValidationException("columnCount must not be negative, but is " + columnCount);
      }
    }
  }

  /**
   * A column of a table.
   *
   * @param tableOid the OID of the table the column belongs to
   * @param name the column name
   * @param nullable false when the column has {@code attnotnull} or its type is a domain whose
   *     chain (domain over domain, down to the base type) has any {@code NOT NULL} domain
   * @param type the column's type
   */
  record ColumnRow(long tableOid, String name, boolean nullable, TypeRow type) {

    /** Validates the components. */
    ColumnRow {
      Validators.isNotEmpty(name, "name");
      Validators.isNotNull(type, "type");
    }
  }

  /**
   * The raw catalog facts about a column's type that {@link PgCatalogMappings#dataType} needs.
   *
   * @param formattedName {@code format_type(atttypid, atttypmod)}, used only for {@code pg_catalog}
   *     types, because for other types it depends on {@code search_path} and quotes identifiers
   * @param schema the schema of the type itself ({@code pg_type.typnamespace})
   * @param name the raw {@code typname}, such as {@code _order_status} for an array type
   * @param elementSchema the element type's schema when the type is an array type; otherwise null
   * @param elementName the element type's raw {@code typname} when the type is an array type;
   *     otherwise null
   */
  record TypeRow(
      String formattedName,
      String schema,
      String name,
      @Nullable String elementSchema,
      @Nullable String elementName) {

    /** Validates the components. */
    TypeRow {
      Validators.isNotNull(formattedName, "formattedName");
      Validators.isNotEmpty(schema, "schema");
      Validators.isNotEmpty(name, "name");
    }
  }

  /**
   * A primary-key ({@code p}), unique ({@code u}) or foreign-key ({@code f}) constraint.
   *
   * @param tableOid the OID of the constrained table
   * @param name the constraint name
   * @param type the constraint type, parsed from {@code contype}
   * @param columns the constrained columns in key order
   * @param referencedTable the referenced table for a foreign key; null otherwise. Resolved from
   *     {@code pg_class} whether or not the target is requested or visible.
   * @param referencedColumns the referenced columns in key order, paired with {@code columns};
   *     empty unless a foreign key
   * @param onDelete the {@code confdeltype} code; meaningful only for a foreign key
   * @param onUpdate the {@code confupdtype} code; meaningful only for a foreign key
   */
  record ConstraintRow(
      long tableOid,
      String name,
      ConstraintType type,
      List<String> columns,
      @Nullable QualifiedName referencedTable,
      List<String> referencedColumns,
      char onDelete,
      char onUpdate) {

    /** Validates and copies the components. */
    ConstraintRow {
      Validators.isNotEmpty(name, "name");
      Validators.isNotNull(type, "type");
      columns = List.copyOf(Validators.containsNoNullElements(columns, "columns"));
      referencedColumns =
          List.copyOf(Validators.containsNoNullElements(referencedColumns, "referencedColumns"));
    }

    /** Creates a new builder for {@link ConstraintRow}. */
    static Builder builder() {
      return new Builder();
    }

    /**
     * Builder for constructing {@link ConstraintRow} instances using a fluent API.
     *
     * <p>The name, type and columns are required. The referenced table defaults to null, the
     * referenced columns to empty, and both actions to {@code a} (no action).
     */
    static final class Builder {
      private long tableOid;
      private @Nullable String name;
      private @Nullable ConstraintType type;
      private @Nullable List<String> columns;
      private @Nullable QualifiedName referencedTable;
      private List<String> referencedColumns = List.of();
      private char onDelete = 'a';
      private char onUpdate = 'a';

      /** Returns this builder for method chaining. */
      Builder tableOid(final long tableOid) {
        this.tableOid = tableOid;
        return this;
      }

      /** Returns this builder for method chaining. */
      Builder name(final String name) {
        this.name = name;
        return this;
      }

      /** Returns this builder for method chaining. */
      Builder type(final ConstraintType type) {
        this.type = type;
        return this;
      }

      /** Returns this builder for method chaining. */
      Builder columns(final List<String> columns) {
        this.columns = columns;
        return this;
      }

      /** Returns this builder for method chaining. */
      Builder referencedTable(final @Nullable QualifiedName referencedTable) {
        this.referencedTable = referencedTable;
        return this;
      }

      /** Returns this builder for method chaining. */
      Builder referencedColumns(final List<String> referencedColumns) {
        this.referencedColumns = referencedColumns;
        return this;
      }

      /** Returns this builder for method chaining. */
      Builder onDelete(final char onDelete) {
        this.onDelete = onDelete;
        return this;
      }

      /** Returns this builder for method chaining. */
      Builder onUpdate(final char onUpdate) {
        this.onUpdate = onUpdate;
        return this;
      }

      /** Builds the {@link ConstraintRow}; validation happens in the compact constructor. */
      ConstraintRow build() {
        return new ConstraintRow(
            tableOid,
            Validators.isNotNull(name, "name"),
            Validators.isNotNull(type, "type"),
            Validators.isNotNull(columns, "columns"),
            referencedTable,
            referencedColumns,
            onDelete,
            onUpdate);
      }
    }
  }

  /**
   * A unique index that qualifies as a unique key: valid, not partial, no expression keys, and not
   * backing a primary-key or unique constraint.
   *
   * @param tableOid the OID of the indexed table
   * @param name the index name
   * @param columns the key columns in key order, without {@code INCLUDE} columns
   */
  record UniqueIndexRow(long tableOid, String name, List<String> columns) {

    /** Validates and copies the components. */
    UniqueIndexRow {
      Validators.isNotEmpty(name, "name");
      columns = List.copyOf(Validators.containsNoNullElements(columns, "columns"));
    }
  }

  /**
   * An enum type.
   *
   * @param schema the schema name
   * @param name the raw {@code typname}
   * @param labels the labels in {@code enumsortorder}
   */
  record EnumRow(String schema, String name, List<String> labels) {

    /** Validates and copies the components. */
    EnumRow {
      Validators.isNotEmpty(schema, "schema");
      Validators.isNotEmpty(name, "name");
      labels = List.copyOf(Validators.containsNoNullElements(labels, "labels"));
    }
  }

  /**
   * Builder for constructing {@link PgCatalogRows} instances using a fluent API.
   *
   * <p>Every list defaults to empty.
   */
  static final class Builder {
    private List<String> requestedSchemas = List.of();
    private List<String> schemaNames = List.of();
    private List<String> schemasWithoutUsage = List.of();
    private List<TableRow> tables = List.of();
    private List<ColumnRow> columns = List.of();
    private List<ConstraintRow> constraints = List.of();
    private List<UniqueIndexRow> uniqueIndexes = List.of();
    private List<EnumRow> enums = List.of();

    /** Returns this builder for method chaining. */
    Builder requestedSchemas(final List<String> requestedSchemas) {
      this.requestedSchemas = requestedSchemas;
      return this;
    }

    /** Returns this builder for method chaining. */
    Builder schemaNames(final List<String> schemaNames) {
      this.schemaNames = schemaNames;
      return this;
    }

    /** Returns this builder for method chaining. */
    Builder schemasWithoutUsage(final List<String> schemasWithoutUsage) {
      this.schemasWithoutUsage = schemasWithoutUsage;
      return this;
    }

    /** Returns this builder for method chaining. */
    Builder tables(final List<TableRow> tables) {
      this.tables = tables;
      return this;
    }

    /** Returns this builder for method chaining. */
    Builder columns(final List<ColumnRow> columns) {
      this.columns = columns;
      return this;
    }

    /** Returns this builder for method chaining. */
    Builder constraints(final List<ConstraintRow> constraints) {
      this.constraints = constraints;
      return this;
    }

    /** Returns this builder for method chaining. */
    Builder uniqueIndexes(final List<UniqueIndexRow> uniqueIndexes) {
      this.uniqueIndexes = uniqueIndexes;
      return this;
    }

    /** Returns this builder for method chaining. */
    Builder enums(final List<EnumRow> enums) {
      this.enums = enums;
      return this;
    }

    /** Builds the {@link PgCatalogRows}; validation happens in the compact constructor. */
    PgCatalogRows build() {
      return new PgCatalogRows(
          requestedSchemas,
          schemaNames,
          schemasWithoutUsage,
          tables,
          columns,
          constraints,
          uniqueIndexes,
          enums);
    }
  }
}
