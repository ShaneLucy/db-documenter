# Catalog model → vendor catalog mapping

Phase 1.2 deliverable (2026-11-27). Maps each public `catalog` component to the source on each 1.0 vendor,
so the model isn't shaped by `pg_catalog` alone. The SQLite column was proven by a spike against SQLite 3.54.1
(sqlite-jdbc 3.54.1.0); the reader needs SQLite **3.37+** for `pragma_table_list`. The MySQL/MariaDB and SQL Server columns are on paper and are verified in Phase 4.
**∅** marks a value that can be empty or unknown on that vendor.

## Mapping

| Component | PostgreSQL (`pg_catalog`) | MySQL / MariaDB (`information_schema`) | SQL Server (`sys.*`) | SQLite (pragmas) |
|---|---|---|---|---|
| `CatalogSnapshot.productName` | `getDatabaseProductName()` = `PostgreSQL` | `MySQL`; MariaDB Connector/J says `MariaDB`, MySQL Connector/J against MariaDB says `MySQL` with `MariaDB` in the version (registry version hook) | `Microsoft SQL Server` | `SQLite` |
| `Schema.name` | `pg_namespace.nspname` | `SCHEMATA.SCHEMA_NAME` (a MySQL *database*) | `sys.schemas.name` within the connected database | `pragma_database_list.name`: `main`, `temp`, attached aliases |
| `Table.name` | `pg_class` `relkind IN ('r','p')`, `NOT relispartition` | `TABLES` `TABLE_TYPE = 'BASE TABLE'` (MariaDB also `SYSTEM VERSIONED`); partitions aren't tables | `sys.tables` `is_ms_shipped = 0` | `pragma_table_list` `type = 'table'`, name not `sqlite_%` (drops `sqlite_schema`, `sqlite_sequence`, `sqlite_stat*`; `virtual` and `shadow` are excluded by type) |
| Visibility | explicit predicate (`has_schema_privilege` + `has_any_column_privilege`) | catalog filters by privilege already | metadata visibility: any permission on the object. `VIEW DEFINITION` is believed needed only for definition text (CHECK bodies, modules), not for key/FK rows (verify) | none: file access is all-or-nothing. `temp` is per-connection, so a fresh connection always sees it empty: read it only when requested |
| `Column` order | `attnum` | `COLUMNS.ORDINAL_POSITION` | `sys.columns.column_id` | `pragma_table_xinfo.cid` (**not** `table_info`, which hides generated columns) |
| `Column.nullable` | `NOT attnotnull` | `IS_NULLABLE = 'YES'` | `is_nullable` | `notnull = 0`, **except** a rowid alias (`INTEGER PRIMARY KEY`, detected as a single-column PK with no `origin='pk'` index), which is never null. A non-integer PK in a rowid table really is nullable |
| `DataType.displayText` | `format_type` for `pg_catalog` types; bare `typname` (+`[]`) for user types | `COLUMNS.COLUMN_TYPE` verbatim (`int unsigned`, `varchar(20)`, `enum('a','b')`) | built from `sys.types.name` + `max_length` (bytes; halve for `n*`; `-1` → `max`), `precision`, `scale` (for `datetime2`/`time`/`datetimeoffset`, `scale` is the fractional-seconds precision); alias types: the bare alias name | declared type verbatim, case preserved; **∅ empty string** for an untyped column |
| `DataType.userType` | enums, domains, composites, ranges (array → element) | ∅ always (no user-defined types) | alias types: `sys.types.is_user_defined = 1` → `(schema_name(schema_id), name)` | ∅ always |
| `PrimaryKey.columns` | `pg_constraint` `contype='p'`, `conkey` with ordinality | `STATISTICS` `INDEX_NAME = 'PRIMARY'` by `SEQ_IN_INDEX` | `sys.key_constraints` `type='PK'` + `sys.index_columns.key_ordinal` | `pragma_table_xinfo.pk` (1-based key position) |
| `PrimaryKey.name` | `conname` | ∅: always `PRIMARY`, so map to empty | `name`; auto-names are `PK__tbl__<random hex>` (`is_system_named = 1`) | ∅: no pragma exposes it |
| `UniqueKey` | constraints `contype='u'` + unique indexes (`indisunique`, valid, no predicate, no expression keys, key atts only) | `STATISTICS` `NON_UNIQUE = 0`, not `PRIMARY`; exclude functional (`EXPRESSION IS NOT NULL`; MariaDB's `STATISTICS` may lack `EXPRESSION`, so verify and add a variant) and **prefix** (`SUB_PART IS NOT NULL`) indexes, which don't make the column unique | `sys.key_constraints type='UQ'` + `sys.indexes is_unique = 1`, not PK/UQ-backing, `has_filter = 0`, `is_disabled = 0`, `is_hypothetical = 0`; key columns only (`is_included_column = 0`) | `pragma_index_list` `unique = 1`, `partial = 0`, `origin <> 'pk'`; columns from `pragma_index_xinfo` `key = 1`; exclude if any `cid = -2` (expression) |
| `UniqueKey.name` | `conname` / index name | index name; auto-named after the first column (`email`, `email_2`), indistinguishable from a chosen name | `name`; auto-names `UQ__tbl__<random hex>` | ∅ for constraints (`sqlite_autoindex_*`, the constraint name is lost); index name for `CREATE UNIQUE INDEX` |
| `ForeignKey` columns + order | `unnest(conkey, confkey) WITH ORDINALITY`, `conparentid = 0` | `KEY_COLUMN_USAGE` `ORDINAL_POSITION`, joined on schema + table + constraint name | `sys.foreign_key_columns.constraint_column_id` | `pragma_foreign_key_list` `id`, `seq` |
| `ForeignKey.referencedTable` | `confrelid` → schema + name | `REFERENCED_TABLE_SCHEMA/NAME`; can be another database | `referenced_object_id`; same database only | `table` column, **same schema as the child** (SQLite FKs can't cross databases), **spelled as the child's DDL wrote it**: fold ASCII case to the declared name when the target exists. Can dangle |
| `ForeignKey.referencedColumns` | `confkey` | `REFERENCED_COLUMN_NAME`. InnoDB accepts an FK to a **non-unique** index, so the target **may not be a key** on MySQL (unless 8.4+ with `restrict_fk_on_non_standard_key` at its default ON) and on MariaDB | `referenced_column_id` | `to`, spelled as the child's DDL wrote it (fold like the table); **NULL when the DDL has no column list** → the target's PK. If the target is missing too, the FK can't be resolved (dropped, logged at WARNING) |
| `ForeignKey.name` | `conname` (auto `t_col_fkey`) | `CONSTRAINT_NAME` (auto `t_ibfk_N`) | `name` (auto `FK__child__col__<random hex>`) | ∅: no pragma exposes it (only `sqlite_master.sql` parsing) |
| `onDelete` / `onUpdate` | `confdeltype` / `confupdtype` codes `a r c n d` | `REFERENTIAL_CONSTRAINTS.DELETE_RULE/UPDATE_RULE`; InnoDB treats `NO ACTION` as `RESTRICT` and rejects `SET DEFAULT` | `delete/update_referential_action_desc`; **no `RESTRICT`** | `on_delete`/`on_update` text; all five values |
| `EnumType` | `pg_enum` by `enumsortorder` | ∅: no named enums; inline `ENUM`/`SET` is in `COLUMN_TYPE` (see below) | ∅ (CHECK constraints only) | ∅ (CHECK constraints only) |
| Bulk (fixed statement count) | ≈6 statements, `nspname = ANY(?)` | one statement per fact kind, `TABLE_SCHEMA IN (?, …)` (parameter count varies, statement count doesn't). MariaDB `information_schema` can be slow on large schemas (verify at 2,000 tables) | one per fact kind, `schema_name IN (…)` | **3–4 statements**: `pragma_table_list t JOIN pragma_table_xinfo(t.name, t.schema)` etc. Correlated table-valued pragmas cover every table in every schema, so **SQLite needs no scale exemption** |

## Model consequences (decided in 1.2)

- **`DataType.displayText` may be empty** (SQLite untyped columns). It is validated non-null only; the renderer
  decides how to show an empty type.
- **`Column.nullable` means "a stored value can be NULL"**, which is the vendor flag except where the flag lies: a SQLite rowid alias (`INTEGER PRIMARY KEY`) reports `notnull = 0` but is never null. Proven in the spike: `INT PRIMARY KEY` and `INTEGER PRIMARY KEY DESC` are not aliases and stay nullable.
- **Key and FK names stay `Optional`.** SQLite exposes none of them, and MySQL's `PRIMARY` carries no information.
  Auto-generated names (SQL Server's random hex, PG's `_fkey`, MySQL's `_ibfk_N`) are kept verbatim: they are real
  names a DBA needs for `ALTER … DROP CONSTRAINT`. The Phase 4 neutral dump omits them. SQL Server's random hex makes a verbatim golden non-deterministic, so the Phase 4 SQL Server fixture **names every constraint explicitly**, plus one IT that matches auto-names by regex. No `isSystemNamed` component (only SQL Server could fill it).
- **A primary-key column can be nullable** (SQLite non-integer PK in a rowid table). Readers report the vendor's
  truth, and Phase 2 cardinality must not assume PK ⇒ NOT NULL.
- **An FK's referenced columns may not form a key** (MySQL < 8.4). Phase 2 cardinality must not assume the target
  side is unique; it should check the target table's PK/unique keys when the target is in the snapshot.
- **`ForeignKey` keeps accepting duplicate local columns.** SQLite accepts `FOREIGN KEY (a, a) REFERENCES p (a, b)`;
  rejecting it would make such a database unreadable. PG rejects it at DDL time, so it never reaches the model. Phase 2 cardinality must treat `(a, a)` as one column. (`PrimaryKey`/`UniqueKey` reject duplicates; `referencedColumns` doesn't.)
- **An unresolvable FK is dropped and logged**, not modelled with empty referenced columns. It only happens on SQLite
  when the DDL has no column list *and* the target table doesn't exist. Allowing empty `referencedColumns` would
  weaken the invariant for every vendor to cover a schema SQLite itself rejects at DML time ("foreign key mismatch"). Logged at WARNING with the table and target.
- **Readers canonicalise identifiers to the declared spelling** when the vendor reports them as written in another
  object's DDL (SQLite FK target table **and** referenced columns), only when the target exists; a dangling target is kept as written. The case-folding rule is per vendor (ASCII-only on SQLite); it belongs to the
  Phase 4 `Dialect`.
- **Left out of 1.0 (user decision):** table kind (PG partitioned, SQLite `WITHOUT ROWID`/`STRICT`, SQL Server/MariaDB
  temporal and history tables), generated columns (SQLite `hidden = 2/3`, MySQL `EXTRA`, SQL Server `is_computed`;
  readers still include them as ordinary columns), and disabled/untrusted FKs (SQL Server `is_disabled`/`is_not_trusted`,
  ≈ PG `NOT VALID`; readers include them). None changes the ERD's boxes, columns, keys or edges. No `vendorProperties`
  map. Adding any of them later is a breaking change (2.0 or new wrapper types).

## Inline `ENUM` / `SET` (MySQL), user decision

`displayText = COLUMN_TYPE` verbatim (`enum('a','b')`, `set('x','y')`), with no new component and no synthetic `EnumType`.
- A synthetic `EnumType` would invent `QualifiedName`s that don't exist in the database, and `SET` is multi-valued, not an enum.
- An `inlineValues` component would be empty on every other vendor. The cross-vendor case doesn't need it, because
  Phase 4's neutral mode drops enum lines and normalises types. CHECK pseudo-enums on other vendors would need CHECK parsing.
- Verbatim text is the only option that can grow without breaking. **Structured values, if ever needed, come as a
  derived helper (e.g. parsing `COLUMN_TYPE`'s `''`-escaped labels), never as a component.** The renderer may truncate long lists.
