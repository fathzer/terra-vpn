package com.fathzer.odvpn.utils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An OutputStream that collects lines into a List.
 * Lines are collected when a newline character is encountered or when the stream is closed.
 */
public abstract class AbstractLineBasedOutputStream extends OutputStream {
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    protected final StringBuilder buffer = new StringBuilder();
    
    protected void doClear() {
        // Does nothing by default
    }

    protected abstract void doFlush();

    /**
     * Clears all collected lines and the buffer.
     */
    public void clear() {
        doClear();
        buffer.setLength(0);
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (buffer) {
            writeByte((byte) b);
        }
    }

    private void writeByte(byte b) {
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
        
        for (int i = off; i < off + len; i++) {
            writeByte(b[i]);
        }
    }

    @Override
    public void flush() {
        if (!buffer.isEmpty()) {
            doFlush();
            buffer.setLength(0);
        }
    }

    @Override
    public void close() {
        flush();
    }
}
