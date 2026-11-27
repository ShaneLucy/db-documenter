package dev.shanelucy.dbdocumenter.internal.reader.postgresql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Captures the records published to one {@code java.util.logging} logger, for tests that assert on
 * warnings. Attach in {@code @BeforeEach} and detach in {@code @AfterEach}; detaching also restores
 * the logger's level.
 */
final class LogCapture extends Handler {

  private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());
  private final SimpleFormatter formatter = new SimpleFormatter();
  private final Logger logger;
  private final Level originalLevel;

  private LogCapture(final Logger logger) {
    this.logger = logger;
    this.originalLevel = logger.getLevel();
  }

  /** Attaches a new capture to the logger with the given name. */
  static LogCapture attach(final String loggerName) {
    final var capture = new LogCapture(Logger.getLogger(loggerName));
    capture.logger.addHandler(capture);
    return capture;
  }

  /** Removes this capture from its logger and restores the logger's level. */
  void detach() {
    logger.removeHandler(this);
    logger.setLevel(originalLevel);
  }

  /** Sets the logger's level, for example to disable warnings. */
  void setLoggerLevel(final Level level) {
    logger.setLevel(level);
  }

  /** Returns every captured record. */
  List<LogRecord> records() {
    return List.copyOf(records);
  }

  /** Returns the captured WARNING messages, formatted with their parameters. */
  List<String> warnings() {
    synchronized (records) {
      return records.stream()
          .filter(logRecord -> logRecord.getLevel() == Level.WARNING)
          .map(formatter::formatMessage)
          .toList();
    }
  }

  @Override
  public void publish(final LogRecord logRecord) {
    records.add(logRecord);
  }

  @Override
  public void flush() {
    // Nothing buffered.
  }

  @Override
  public void close() {
    // Nothing to release.
  }
}
