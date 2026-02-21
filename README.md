# db-documenter

A library that generates [PlantUML](https://plantuml.com/) entity-relationship diagrams by introspecting live database schemas.

---

## Database Support

The tables below document which schema features are supported for each database. This is a reference for both **consumers** evaluating db-documenter and **maintainers** tracking feature coverage across database implementations.

### Legend

| Symbol | Meaning |
|:------:|---------|
| ✅ | Fully supported |
| 🔜 | Planned |
| ❌ | Not supported |

---

### Schema Objects

Which top-level database objects are discovered and rendered.

| Feature | PostgreSQL |
|---------|:----------:|
| Regular tables | ✅ |
| Partitioned tables (with partition key) | ✅ |
| Partition children (listed under parent) | ✅ |
| Views | ✅ |
| Materialized views | ✅ |
| Enums | ✅ |
| Composite types | ✅ |
| Standalone sequences | ❌ |
| Non-constraint indexes | ❌ |
| Domain types | ❌ |
| Foreign tables (FDW) | ❌ |
| Functions / stored procedures | ❌ |
| Triggers | ❌ |

---

### Column Metadata

Which column-level properties are captured and included in the diagram output.

| Feature | PostgreSQL |
|---------|:----------:|
| Data type | ✅ |
| Nullability | ✅ |
| Primary key | ✅ |
| Foreign key | ✅ |
| Unique constraint (single-column) | ✅ |
| Unique constraint (composite, with constraint name) | ✅ |
| Auto-increment / sequence (`nextval`) | ✅ |
| Default value | ✅ |
| Check constraint | ✅ |
| Generated column | ✅ |
| Array type | ✅ |
| User-defined type (enum / composite reference) | ✅ |
| Character maximum length | ✅ |
| Numeric precision and scale | ✅ |
| Column comments | ❌ |
| Collation | ❌ |

---

### Relationships

How foreign key relationships are represented in the output diagram.

| Feature | PostgreSQL |
|---------|:----------:|
| Foreign key relationships | ✅ |
| Relationship cardinality | ✅ |
| Cross-schema relationships | ✅ |
| ON DELETE action | ✅ |
| ON UPDATE action | ✅ |
