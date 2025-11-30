package db.documenter.internal.builder;

import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.queries.api.QueryRunner;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Builds database enum type information from schema metadata. */
public final class EnumBuilder {

  /**
   * Builds a list of database enum types with their values for a given schema.
   *
   * @param queryRunner the query runner to fetch enum metadata
   * @param schema the schema name
   * @return list of {@link DbEnum} instances
   * @throws SQLException if database access fails
   */
  public List<DbEnum> buildEnums(final QueryRunner queryRunner, final String schema)
      throws SQLException {
    final List<DbEnum> dbEnums = queryRunner.getEnumInfo(schema);
    final List<DbEnum> result = new ArrayList<>();

    for (final DbEnum dbEnum : dbEnums) {
      final List<String> dbEnumValues = queryRunner.getEnumValues(schema, dbEnum);
      result.add(
          DbEnum.builder()
              .enumName(dbEnum.enumName())
              .columnName(dbEnum.columnName())
              .enumValues(dbEnumValues)
              .build());
    }

    return result;
  }
}
