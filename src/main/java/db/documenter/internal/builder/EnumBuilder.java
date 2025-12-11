package db.documenter.internal.builder;

import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.queries.api.QueryRunner;
import db.documenter.internal.utils.LogUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Builds database enum type information from schema metadata. */
public final class EnumBuilder {
  private static final Logger LOGGER = Logger.getLogger(EnumBuilder.class.getName());

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

    if (LOGGER.isLoggable(Level.INFO)) {
      LOGGER.log(Level.INFO, "Building enums for schema: {0}", LogUtils.sanitizeForLog(schema));
    }

    final List<DbEnum> result = new ArrayList<>();

    for (final DbEnum dbEnum : dbEnums) {
      final List<String> dbEnumValues = queryRunner.getEnumValues(dbEnum.schemaName(), dbEnum);
      result.add(
          DbEnum.builder()
              .schemaName(dbEnum.schemaName())
              .enumName(dbEnum.enumName())
              .enumValues(dbEnumValues)
              .build());
    }

    return result;
  }
}
