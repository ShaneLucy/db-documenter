package db.documenter.cli;

import db.documenter.internal.exceptions.ValidationException;
import java.nio.file.Path;

/**
 * Derives the enum diagram output path from the ERD output path.
 *
 * <p>Pure path arithmetic with no I/O so the naming rules can be tested in isolation.
 */
final class EnumOutputPaths {
  private static final String SUFFIX = "_enums";

  private EnumOutputPaths() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Appends {@code _enums} to the file name of the given path, before its extension.
   *
   * <p>Only the file name is inspected, so dots in directory names are ignored. A name whose only
   * dot is the leading one (e.g. {@code .puml}) has no extension. The result is a sibling of {@code
   * erdPath}, or relative to the current directory when {@code erdPath} has no parent. Examples:
   * {@code schema.puml} to {@code schema_enums.puml}, {@code out} to {@code out_enums}, {@code
   * a.b.puml} to {@code a.b_enums.puml}.
   *
   * @param erdPath path the ERD is written to; must have a file name
   * @return path the enum diagram is written to
   * @throws ValidationException if {@code erdPath} has no file name (e.g. a filesystem root)
   */
  static Path derive(final Path erdPath) {
    final var fileNamePath = erdPath.getFileName();
    if (fileNamePath == null) {
      throw new ValidationException("erdPath file name must not be null");
    }
    final var fileName = fileNamePath.toString();
    final int extensionIndex = fileName.lastIndexOf('.');
    final var enumFileName =
        extensionIndex > 0
            ? fileName.substring(0, extensionIndex) + SUFFIX + fileName.substring(extensionIndex)
            : fileName + SUFFIX;

    final var parent = erdPath.getParent();
    return parent == null ? Path.of(enumFileName) : parent.resolve(enumFileName);
  }
}
