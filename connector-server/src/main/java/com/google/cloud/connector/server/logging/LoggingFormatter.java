package com.google.cloud.connector.server.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Logging Formatter to append thread id to the logs.
 */
public class LoggingFormatter extends SimpleFormatter {

  @Override
  public String format(LogRecord record) {
    return String.format("[%s] %s: %s\n", Thread.currentThread().getName(),
        record.getLevel().getName(), super.format(record));
  }
}
