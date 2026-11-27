package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import dev.shanelucy.dbdocumenter.internal.utils.LogUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The warnings the assemblers log for rows they leave out of a snapshot.
 *
 * <p>Both assemblers log through the {@link PgCatalogAssembler} logger, so one logger name covers
 * everything a read leaves out. Every argument is passed through {@link LogUtils#sanitizeForLog},
 * because catalog names may contain line breaks.
 */
final class PgCatalogLog {

  private static final Logger LOGGER = Logger.getLogger(PgCatalogAssembler.class.getName());

  private PgCatalogLog() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Logs a row dropped because it no longer fits the other rows, typically after concurrent DDL.
   *
   * @param kind what was dropped, such as {@code column} or {@code unique index}
   * @param name the dropped object's name, for the message
   * @param reason why, completing "because …"
   */
  static void dropped(final String kind, final String name, final String reason) {
    if (LOGGER.isLoggable(Level.WARNING)) {
      LOGGER.log(
          Level.WARNING,
          "Dropped {0} {1} because {2}; the catalog probably changed during the read",
          new Object[] {
            LogUtils.sanitizeForLog(kind),
            LogUtils.sanitizeForLog(name),
            LogUtils.sanitizeForLog(reason)
          });
    }
  }

  /**
   * Logs a row left out because the model cannot represent it, not because of concurrent DDL.
   *
   * @param kind what was left out
   * @param name its name, for the message
   * @param reason why, completing "because …"
   */
  static void skipped(final String kind, final String name, final String reason) {
    if (LOGGER.isLoggable(Level.WARNING)) {
      LOGGER.log(
          Level.WARNING,
          "Skipped {0} {1} because {2}",
          new Object[] {
            LogUtils.sanitizeForLog(kind),
            LogUtils.sanitizeForLog(name),
            LogUtils.sanitizeForLog(reason)
          });
    }
  }

  /**
   * Logs a requested schema that is absent from the snapshot.
   *
   * @param schema the requested schema name
   * @param reason why it is absent, such as "it does not exist"
   */
  static void schemaAbsent(final String schema, final String reason) {
    if (LOGGER.isLoggable(Level.WARNING)) {
      LOGGER.log(
          Level.WARNING,
          "Requested schema {0} is not in the snapshot because {1}",
          new Object[] {LogUtils.sanitizeForLog(schema), LogUtils.sanitizeForLog(reason)});
    }
  }
}
