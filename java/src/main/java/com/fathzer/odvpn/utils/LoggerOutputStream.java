package com.fathzer.odvpn.utils;

import org.slf4j.Logger;

/**
 * An OutputStream that writes to an SLF4J Logger at a specified log level.
 */
public class LoggerOutputStream extends AbstractLineBasedOutputStream {
    private final Logger logger;
    private final LogLevel level;

    /**
     * Available log levels.
     */
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    /**
     * Creates a new LoggerOutputStream.
     *
     * @param logger The SLF4J Logger to write to
     * @param level  The log level to use when writing to the logger
     * @throws IllegalArgumentException if logger is null or level is null
     */
    public LoggerOutputStream(Logger logger, LogLevel level) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }
        if (level == null) {
            throw new IllegalArgumentException("LogLevel cannot be null");
        }
        this.logger = logger;
        this.level = level;
    }

    @Override
    protected void synchronizedClear() {
        // No additional actions should be done to clear the logger
    }

    @Override
    protected void synchronizedFlush() {
        final String message = buffer.toString();
        
        switch (level) {
            case TRACE:
                logger.trace(message);
                break;
            case DEBUG:
                logger.debug(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case ERROR:
                logger.error(message);
                break;
        }
    }
}
