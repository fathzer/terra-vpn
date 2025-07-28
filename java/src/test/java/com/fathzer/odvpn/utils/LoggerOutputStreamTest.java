package com.fathzer.odvpn.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;

class LoggerOutputStreamTest {
    @Test
    void testLogAtEachLevel() throws IOException {
        Logger logger = mock(Logger.class);
        for (LoggerOutputStream.LogLevel level : LoggerOutputStream.LogLevel.values()) {
            try (LoggerOutputStream out = new LoggerOutputStream(logger, level)) {
                out.write("Hello\nWorld".getBytes());
                out.flush(); // Should log "World" as well
            }
            verifyLog(logger, level, "Hello");
            verifyLog(logger, level, "World");
            reset(logger);
        }
    }

    @Test
    void testFlushOnClose() throws IOException {
        Logger logger = mock(Logger.class);
        try (LoggerOutputStream out = new LoggerOutputStream(logger, LoggerOutputStream.LogLevel.INFO)) {
            out.write("foo".getBytes());
        }
        verify(logger).info("foo");
    }

    @Test
    void testCarriageReturnIgnored() throws IOException {
        Logger logger = mock(Logger.class);
        try (LoggerOutputStream out = new LoggerOutputStream(logger, LoggerOutputStream.LogLevel.ERROR)) {
            out.write("abc\r\ndef\r\n".getBytes());
        }
        verify(logger).error("abc");
        verify(logger).error("def");
    }

    @Test
    void testNullLoggerOrLevelThrows() {
        assertThrows(IllegalArgumentException.class, () -> new LoggerOutputStream(null, LoggerOutputStream.LogLevel.INFO));
        Logger logger = mock(Logger.class);
        assertThrows(IllegalArgumentException.class, () -> new LoggerOutputStream(logger, null));
    }

    private void verifyLog(Logger logger, LoggerOutputStream.LogLevel level, String message) {
        switch (level) {
            case TRACE: verify(logger).trace(message); break;
            case DEBUG: verify(logger).debug(message); break;
            case INFO: verify(logger).info(message); break;
            case WARN: verify(logger).warn(message); break;
            case ERROR: verify(logger).error(message); break;
        }
    }
}
