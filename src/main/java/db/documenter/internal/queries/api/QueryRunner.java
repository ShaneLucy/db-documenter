package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.postgresql.UdtReference;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface QueryRunner {

  List<Table> getTableInfo(String schema) throws SQLException;

  List<Column> getColumnInfo(String schema, Table table) throws SQLException;

  PrimaryKey getPrimaryKeyInfo(String schema, Table table) throws SQLException;

  List<ForeignKey> getForeignKeyInfo(String schema, Table table) throws SQLException;

  List<DbEnum> getEnumInfo(String schema) throws SQLException;

  List<String> getEnumValues(String schema, DbEnum dbEnums) throws SQLException;

  /**
   * Retrieves mappings of columns to their user-defined types (enums/composite types).
   *
   * <p>Used to resolve which columns reference which types across schemas. This enables proper
   * cross-schema type resolution where a column in one schema may reference a UDT defined in
   * another schema.
   *
   * <p><b>Cross-Schema Resolution:</b> The returned map allows looking up the defining schema and
   * type name for any column that uses a user-defined type, even when the type is defined in a
   * different schema than the table.
   *
   * <p><b>Example:</b>
   *
   * <pre>{@code
   * // Get UDT mappings for 'core' schema
   * Map<ColumnKey, UdtReference> mappings = queryRunner.getColumnUdtMappings("core");
   *
   * // Look up where the 'status' column's type is defined
   * ColumnKey key = new ColumnKey("users", "status");
   * UdtReference udtRef = mappings.get(key);
   * // udtRef might be UdtReference("auth", "account_status")
   * // meaning the type is defined in 'auth' schema, not 'core'
   * }</pre>
   *
   * @param schemaName the schema containing the tables to query
   * @return immutable map of column keys to UDT references; never null, may be empty if no UDTs are
   *     used
   * @throws SQLException if database query fails
   */
  Map<ColumnKey, UdtReference> getColumnUdtMappings(String schemaName) throws SQLException;

  /**
   * Retrieves all composite types defined in the specified schema.
   *
   * <p>Composite types are user-defined types created with {@code CREATE TYPE ... AS (...)} that
   * define structured types with multiple named fields. These are distinct from tables in that they
   * are type definitions, not data containers.
   *
   * <p><b>PlantUML Rendering:</b> Composite types are rendered with the {@code <<composite>>}
   * stereotype to visually distinguish them from tables and enums.
   *
   * <p><b>Example:</b>
   *
   * <pre>{@code
   * // PostgreSQL: CREATE TYPE core.address AS (
   * //   street varchar(200),
   * //   city varchar(100),
   * //   postal_code varchar(10)
   * // );
   * List<DbCompositeType> compositeTypes = queryRunner.getCompositeTypeInfo("core");
   * // Returns list containing the 'address' composite type with its 3 fields
   * }</pre>
   *
   * @param schema the schema to query for composite types
   * @return immutable list of composite types; never null, may be empty if no composite types exist
   * @throws SQLException if database query fails
   */
  List<DbCompositeType> getCompositeTypeInfo(String schema) throws SQLException;
}
