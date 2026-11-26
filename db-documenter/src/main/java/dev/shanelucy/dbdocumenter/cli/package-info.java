/**
 * CLI entry point for the db-documenter tool.
 *
 * <p>This package contains the picocli-based command-line interface that bridges user-supplied
 * arguments to the {@link dev.shanelucy.dbdocumenter.DbDocumenter} API. It is responsible for
 * argument parsing, interactive password prompting, output routing (file or stdout), and mapping
 * exceptions to meaningful exit codes.
 *
 * @see dev.shanelucy.dbdocumenter.DbDocumenter
 * @see dev.shanelucy.dbdocumenter.DbDocumenterConfig
 */
package dev.shanelucy.dbdocumenter.cli;
