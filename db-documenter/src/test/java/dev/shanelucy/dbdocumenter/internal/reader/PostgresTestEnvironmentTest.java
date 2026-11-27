package dev.shanelucy.dbdocumenter.internal.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.shanelucy.dbdocumenter.testhelpers.PostgresTestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the test-support {@link PostgresTestEnvironment} image selection; no container is
 * started.
 *
 * <p>Lives in a library package rather than {@code dev.shanelucy.dbdocumenter.testhelpers} for the
 * same reason as {@link QueryCountingConnectionTest}: a library test package of that name would
 * split-shadow test-support's package. It sets a JVM-wide system property, which is restored after
 * each test; only integration tests (a separate failsafe JVM) read it otherwise.
 */
class PostgresTestEnvironmentTest {

  private String originalImage;

  @BeforeEach
  void setUp() {
    originalImage = System.getProperty(PostgresTestEnvironment.IMAGE_PROPERTY);
  }

  @AfterEach
  void tearDown() {
    if (originalImage == null) {
      System.clearProperty(PostgresTestEnvironment.IMAGE_PROPERTY);
    } else {
      System.setProperty(PostgresTestEnvironment.IMAGE_PROPERTY, originalImage);
    }
  }

  @Nested
  class ConfiguredImageTests {

    @Test
    void itDefaultsToDefaultImageWhenPropertyIsUnset() {
      System.clearProperty(PostgresTestEnvironment.IMAGE_PROPERTY);

      assertEquals(
          PostgresTestEnvironment.DEFAULT_IMAGE, PostgresTestEnvironment.configuredImage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void itDefaultsToDefaultImageWhenPropertyIsBlank(final String blank) {
      System.setProperty(PostgresTestEnvironment.IMAGE_PROPERTY, blank);

      assertEquals(
          PostgresTestEnvironment.DEFAULT_IMAGE, PostgresTestEnvironment.configuredImage());
    }

    @Test
    void itReturnsStrippedPropertyValueWhenSet() {
      System.setProperty(PostgresTestEnvironment.IMAGE_PROPERTY, " postgres:18 ");

      assertEquals("postgres:18", PostgresTestEnvironment.configuredImage());
    }

    @Test
    void itPinsOldestAndLatestSupportedMajorsToMinorReleases() {
      assertEquals("postgres:15.20", PostgresTestEnvironment.DEFAULT_IMAGE);
      assertEquals("postgres:18.7", PostgresTestEnvironment.LATEST_SUPPORTED_IMAGE);
    }

    @Test
    void itUsesDocumentedPropertyName() {
      assertEquals("db-documenter.test.postgres.image", PostgresTestEnvironment.IMAGE_PROPERTY);
    }
  }

  @Nested
  class ConstructorTests {

    @Test
    void itUsesConfiguredImageForNoArgumentConstructor() {
      System.setProperty(PostgresTestEnvironment.IMAGE_PROPERTY, "postgres:18");

      assertEquals("postgres:18", new PostgresTestEnvironment().getImage());
    }

    @Test
    void itUsesGivenImageAndIgnoresProperty() {
      System.setProperty(PostgresTestEnvironment.IMAGE_PROPERTY, "postgres:18");

      assertEquals("postgres:16", new PostgresTestEnvironment("postgres:16").getImage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void itThrowsExceptionWhenImageIsBlank(final String image) {
      final var exception =
          assertThrows(IllegalArgumentException.class, () -> new PostgresTestEnvironment(image));

      assertEquals("image must not be blank", exception.getMessage());
    }
  }
}
