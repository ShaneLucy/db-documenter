package db.documenter.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ManifestVersionProviderTest {

  @Nested
  class GetVersionTests {

    @Test
    void whenManifestHasNoVersionReturnsDevelopmentBuildFallback() {
      final var version = new ManifestVersionProvider().getVersion();

      assertArrayEquals(new String[] {ManifestVersionProvider.DEVELOPMENT_VERSION}, version);
    }
  }

  @Nested
  class DescribeTests {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void whenVersionIsMissingReturnsDevelopmentBuild(final String version) {
      final var result = ManifestVersionProvider.describe("db-documenter", version);

      assertEquals(ManifestVersionProvider.DEVELOPMENT_VERSION, result);
    }

    @Test
    void itCombinesTitleAndVersion() {
      final var result = ManifestVersionProvider.describe("db-documenter", "0.2.0");

      assertEquals("db-documenter 0.2.0", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void whenTitleIsMissingUsesDefaultTitle(final String title) {
      final var result = ManifestVersionProvider.describe(title, "0.2.0");

      assertEquals("db-documenter 0.2.0", result);
    }

    @ParameterizedTest
    @CsvSource({
      "custom-title, 1.0.0, custom-title 1.0.0",
      "db-documenter, 0.2.0-rc.1, db-documenter 0.2.0-rc.1"
    })
    void itUsesManifestTitleAndVersionVerbatim(
        final String title, final String version, final String expected) {
      final var result = ManifestVersionProvider.describe(title, version);

      assertEquals(expected, result);
    }
  }
}
