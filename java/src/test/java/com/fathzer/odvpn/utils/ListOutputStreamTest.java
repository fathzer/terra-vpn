package com.fathzer.odvpn.utils;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListOutputStreamTest {
    @Test
    void testWriteSingleBytesAndGetLines() throws IOException {
        try (ListOutputStream los = new ListOutputStream()) {
            // Test with ending /n
            for (char c : "Hello\nWorld\n".toCharArray()) {
                los.write((byte) c);
            }
            assertEquals(List.of("Hello", "World"), los.getLines());

            // Test clear
            los.clear();
            assertTrue(los.getLines().isEmpty());

            // Test without ending /n
            byte[] bytes = "foo\nbar\nbaz".getBytes(StandardCharsets.UTF_8);
            los.write(bytes, 0, bytes.length);
            assertEquals(List.of("foo", "bar", "baz"), los.getLines());

            // Test empty write
            los.clear();
            los.write(new byte[0]);
            los.flush();
            assertTrue(los.getLines().isEmpty());

            // Test null write
            los.clear();
            assertThrows(NullPointerException.class, () -> los.write(null, 0, 1));

            // Test invalid offsets
            byte[] arr = "abc".getBytes(StandardCharsets.UTF_8);
            assertThrows(IndexOutOfBoundsException.class, () -> los.write(arr, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> los.write(arr, 0, 10));
        }
    }

    @Test
    void testWriteCarriageReturnIgnored() throws IOException {
        try (ListOutputStream los = new ListOutputStream()) {
            los.write("a\r\nb\r\n".getBytes(StandardCharsets.UTF_8));
            assertEquals(List.of("a", "b"), los.getLines());
        }
    }

    @Test
    void testCloseFlushesBuffer() throws IOException {
        final ListOutputStream los = new ListOutputStream();
        try (los) {
            los.write("incomplete".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(List.of("incomplete"), los.getLines());
    }
}
