package com.fathzer.odvpn.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An OutputStream that collects lines into a List.
 * Lines are collected when a newline character is encountered or when the stream is closed.
 */
public class ListOutputStream extends AbstractLineBasedOutputStream {
    private final List<String> lines = new ArrayList<>();

    /**
     * Gets an unmodifiable view of the collected lines.
     * @return the collected lines
     */
    public List<String> getLines() {
        flush();
        return Collections.unmodifiableList(lines);
    }

    @Override
    protected void doFlush() {
        lines.add(buffer.toString());
    }

    @Override
    protected void doClear() {
        lines.clear();
    }
}
