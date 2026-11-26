package dev.shanelucy.dbdocumenter.testhelpers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Reads shared fixtures and golden files from the classpath so that tests in any module can use
 * them without depending on the working directory.
 */
public final class TestResources {

  private TestResources() {}

  /**
   * Reads a classpath resource as UTF-8, matching the decoding of {@code Files.readString}.
   *
   * @param resourcePath absolute classpath path such as {@code /postgresql/single-schema/x.puml}
   * @return the resource contents, unmodified
   * @throws IllegalArgumentException if the resource does not exist
   */
  public static String readString(final String resourcePath) {
    try (var is = TestResources.class.getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Test resource not found: " + resourcePath);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read test resource: " + resourcePath, e);
    }
  }
}
