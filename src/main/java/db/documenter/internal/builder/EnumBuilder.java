package db.documenter.internal.builder;

import db.documenter.internal.models.db.DbEnum;
import db.documenter.internal.models.db.postgresql.EnumKey;
import db.documenter.internal.queries.impl.postgresql.PostgresqlQueryRunner;
import db.documenter.internal.utils.LogUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
  public List<DbEnum> buildEnums(final PostgresqlQueryRunner queryRunner, final String schema)
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

  public Map<EnumKey, DbEnum> buildEnumKeys(final List<DbEnum> dbEnums, final String schema) {
    final Map<EnumKey, DbEnum> enumsByKey =
        dbEnums.stream()
            .collect(
                Collectors.toMap(
                    dbEnum -> new EnumKey(dbEnum.schemaName(), dbEnum.enumName()),
                    Function.identity()));

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.log(
          Level.FINE,
          "Created enum lookup map with {0} entries for schema: {1}",
          new Object[] {enumsByKey.size(), LogUtils.sanitizeForLog(schema)});
    }

    return enumsByKey;
  }
}
