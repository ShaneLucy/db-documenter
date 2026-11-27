package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.logging.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PgCatalogLogTest {

  private LogCapture logCapture;

  @BeforeEach
  void setUp() {
    logCapture = LogCapture.attach(PgCatalogAssembler.class.getName());
  }

  @AfterEach
  void tearDown() {
    logCapture.detach();
  }

  @Nested
  class DroppedTests {

    @Test
    void itLogsDroppedRowUnderAssemblerLogger() {
      PgCatalogLog.dropped("column", "email", "its table (OID 7) was not read");

      assertEquals(
          List.of(
              "Dropped column email because its table (OID 7) was not read; the catalog probably"
                  + " changed during the read"),
          logCapture.warnings());
    }

    @Test
    void itSanitisesEveryArgument() {
      PgCatalogLog.dropped("uni\nque", "a\r\nb", "x\ny");

      assertEquals(
          List.of("Dropped uni que a b because x y; the catalog probably changed during the read"),
          logCapture.warnings());
    }

    @Test
    void itLogsNothingWhenWarningsAreDisabled() {
      logCapture.setLoggerLevel(Level.SEVERE);

      PgCatalogLog.dropped("column", "email", "reason");

      assertEquals(List.of(), logCapture.records());
    }
  }

  @Nested
  class SkippedTests {

    @Test
    void itLogsSkippedRowWithoutConcurrentDdlHint() {
      PgCatalogLog.skipped("key", "core.t t_aa", "it lists a column more than once");

      assertEquals(
          List.of("Skipped key core.t t_aa because it lists a column more than once"),
          logCapture.warnings());
    }

    @Test
    void itSanitisesEveryArgument() {
      PgCatalogLog.skipped("k\ney", "na\rme", "rea\nson");

      assertEquals(List.of("Skipped k ey name because rea son"), logCapture.warnings());
    }

    @Test
    void itLogsNothingWhenWarningsAreDisabled() {
      logCapture.setLoggerLevel(Level.OFF);

      PgCatalogLog.skipped("key", "t", "reason");

      assertEquals(List.of(), logCapture.records());
    }
  }

  @Nested
  class SchemaAbsentTests {

    @Test
    void itLogsAbsentSchema() {
      PgCatalogLog.schemaAbsent("nope", "it does not exist");

      assertEquals(
          List.of("Requested schema nope is not in the snapshot because it does not exist"),
          logCapture.warnings());
    }

    @Test
    void itSanitisesEveryArgument() {
      PgCatalogLog.schemaAbsent("a\nb", "c\rd");

      assertEquals(
          List.of("Requested schema a b is not in the snapshot because cd"), logCapture.warnings());
    }

    @Test
    void itLogsNothingWhenWarningsAreDisabled() {
      logCapture.setLoggerLevel(Level.SEVERE);

      PgCatalogLog.schemaAbsent("nope", "it does not exist");

      assertEquals(List.of(), logCapture.records());
    }
  }
}
