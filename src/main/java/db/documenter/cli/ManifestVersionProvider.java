package db.documenter.cli;

import org.jspecify.annotations.Nullable;
import picocli.CommandLine.IVersionProvider;

/**
 * Supplies the {@code --version} text from the jar manifest so the Maven POM stays the single
 * source of truth for the release version.
 *
 * <p>When running from unpackaged classes (tests, IDE) the manifest is absent, so a fixed
 * development-build string is returned instead; the result is never null.
 */
final class ManifestVersionProvider implements IVersionProvider {
  static final String DEVELOPMENT_VERSION = "db-documenter (development build)";

  private static final String DEFAULT_TITLE = "db-documenter";

  @Override
  public String[] getVersion() {
    final var cliPackage = Main.class.getPackage();
    return new String[] {
      describe(cliPackage.getImplementationTitle(), cliPackage.getImplementationVersion())
    };
  }

  /**
   * Builds the version line from manifest attributes, either of which may be missing.
   *
   * <p>Kept separate from {@link #getVersion()} because the manifest is only present in the
   * packaged jar, so this is the only way to test the packaged-jar behaviour.
   *
   * @param title the {@code Implementation-Title}; falls back to {@code db-documenter} when blank
   * @param version the {@code Implementation-Version}; when blank the development-build text is
   *     returned
   * @return {@code "<title> <version>"}, or {@link #DEVELOPMENT_VERSION} when there is no version
   */
  static String describe(final @Nullable String title, final @Nullable String version) {
    if (version == null || version.isBlank()) {
      return DEVELOPMENT_VERSION;
    }
    final var name = title == null || title.isBlank() ? DEFAULT_TITLE : title;
    return name + " " + version;
  }
}
