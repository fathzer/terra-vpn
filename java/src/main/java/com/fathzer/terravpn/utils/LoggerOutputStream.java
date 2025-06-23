package com.fathzer.terravpn.utils;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * An OutputStream that writes to an SLF4J Logger at a specified log level.
 */
public class LoggerOutputStream extends OutputStream {
    private final Logger logger;
    private final LogLevel level;
    private final StringBuilder buffer;
    private static final int BUFFER_SIZE = 1024;

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
        this.buffer = new StringBuilder(BUFFER_SIZE);
    }

    @Override
    public void write(int b) throws IOException {
        // Convert byte to char and add to buffer
        char c = (char) (b & 0xFF);
        
        // Check for newline or buffer full
        if (c == '\n' || buffer.length() >= BUFFER_SIZE) {
            flush();
        } else if (c != '\r') {  // Ignore carriage return
            buffer.append(c);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        
        String str = new String(b, off, len, StandardCharsets.UTF_8);
        int lastNewline = 0;
        int i;
        
        // Process each line in the input
        for (i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '\n') {
                buffer.append(str, lastNewline, i);
                flush();
                lastNewline = i + 1;
            }
        }
        
        // Add remaining characters to buffer (no newline at end)
        if (lastNewline < i) {
            buffer.append(str, lastNewline, i);
        }
    }

    @Override
    public void flush() {
        if (buffer.length() > 0) {
            String message = buffer.toString();
            buffer.setLength(0);
            
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

    @Override
    public void close() {
        flush();
    }
}
