package com.fathzer.terravpn.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An OutputStream that collects lines into a List.
 * Lines are collected when a newline character is encountered or when the stream is closed.
 */
public class ListOutputStream extends OutputStream {
    private final List<String> lines = new ArrayList<>();
    private final StringBuilder buffer = new StringBuilder();
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    /**
     * Gets an unmodifiable view of the collected lines.
     * @return the collected lines
     */
    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Clears all collected lines and the buffer.
     */
    public void clear() {
        synchronized (buffer) {
            buffer.setLength(0);
            lines.clear();
        }
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (buffer) {
            // Convert byte to char and add to buffer
            char c = (char) (b & 0xFF);
            
            // Check for newline or buffer full
            if (c == '\n' || buffer.length() >= BUFFER_SIZE) {
                flushBuffer();
            } else if (c != '\r') {  // Ignore carriage return
                buffer.append(c);
            }
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
        
        synchronized (buffer) {
            String str = new String(b, off, len, StandardCharsets.UTF_8);
            int lastNewline = 0;
            
            // Process each line in the input
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == '\n') {
                    buffer.append(str, lastNewline, i);
                    flushBuffer();
                    lastNewline = i + 1;
                }
            }
            
            // Add remaining characters to buffer (no newline at end)
            if (lastNewline < str.length()) {
                buffer.append(str, lastNewline, str.length());
            }
        }
    }

    @Override
    public void flush() {
        flushBuffer();
    }

    @Override
    public void close() {
        flush();
    }

    /**
     * Flushes the current buffer content to the lines list.
     */
    private void flushBuffer() {
        synchronized (buffer) {
            if (buffer.length() > 0) {
                lines.add(buffer.toString());
                buffer.setLength(0);
            }
        }
    }
}
