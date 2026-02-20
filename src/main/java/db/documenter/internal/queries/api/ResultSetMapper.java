package db.documenter.internal.queries.api;

import db.documenter.internal.models.db.Column;
import db.documenter.internal.models.db.ColumnKey;
import db.documenter.internal.models.db.DbCompositeType;
import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.ForeignKey;
import db.documenter.internal.models.db.MaterializedView;
import db.documenter.internal.models.db.PrimaryKey;
import db.documenter.internal.models.db.Table;
import db.documenter.internal.models.db.View;
import db.documenter.internal.models.db.postgresql.UdtReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Contract for mapping JDBC {@link ResultSet} rows to domain model objects.
 *
 * <p>Implementations extract column values from the result set and construct immutable domain
 * records. Each method assumes the result set cursor is positioned before the first row (i.e.,
 * iteration starts from the beginning).
 *
 * <p><b>Note on stub objects:</b> Some methods (e.g., {@link #mapToTables}, {@link #mapToViews},
 * {@link #mapToMaterializedViews}) return objects with only the name populated and an empty column
 * list. Columns are enriched in a subsequent query by the builder layer, following the two-phase
 * construction pattern.
 *
 * @see db.documenter.internal.queries.impl.postgresql.resultsets.PostgresqlResultSetMapper
 */
public interface ResultSetMapper {

  List<Table> mapToTables(ResultSet resultSet) throws SQLException;

  List<Column> mapToColumns(ResultSet resultSet) throws SQLException;

  PrimaryKey mapToPrimaryKey(ResultSet resultSet) throws SQLException;

  List<ForeignKey> mapToForeignKeys(ResultSet resultSet) throws SQLException;

  List<DbEnum> mapToDbEnumInfo(ResultSet resultSet) throws SQLException;

  List<String> mapToDbEnumValues(ResultSet resultSet) throws SQLException;

  Map<ColumnKey, UdtReference> mapToColumnUdtMappings(ResultSet resultSet) throws SQLException;

  List<DbCompositeType> mapToDbCompositeTypeInfo(ResultSet resultSet) throws SQLException;

  /**
   * Maps result set rows to stub {@link View} objects containing only the view name.
   *
   * <p>Each row must contain a {@code table_name} column. The returned views have an empty column
   * list; columns are populated by the builder in a subsequent {@code getColumnInfo} call.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of stub views; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  List<View> mapToViews(ResultSet resultSet) throws SQLException;

  /**
   * Maps result set rows to stub {@link MaterializedView} objects containing only the view name.
   *
   * <p>Each row must contain a {@code table_name} column. The returned materialized views have an
   * empty column list; columns are populated by the builder in a subsequent {@code getColumnInfo}
   * call.
   *
   * @param resultSet the result set positioned before the first row
   * @return list of stub materialized views; never null, may be empty
   * @throws SQLException if a database access error occurs
   */
  List<MaterializedView> mapToMaterializedViews(ResultSet resultSet) throws SQLException;
}
