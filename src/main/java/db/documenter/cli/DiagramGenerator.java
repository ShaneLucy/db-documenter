package db.documenter.cli;

import db.documenter.DbDocumenterConfig;
import db.documenter.DiagramResult;
import java.sql.SQLException;

/**
 * Seam between the CLI and {@link db.documenter.DbDocumenter} so exit-code mapping can be tested
 * without a database.
 */
@FunctionalInterface
interface DiagramGenerator {

  /**
   * Generates the diagrams for the given configuration.
   *
   * @param config validated configuration
   * @return the generated diagrams
   * @throws SQLException if database access fails
   */
  DiagramResult generate(DbDocumenterConfig config) throws SQLException;
}
