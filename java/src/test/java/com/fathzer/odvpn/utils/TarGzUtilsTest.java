package com.fathzer.odvpn.utils;

import static org.junit.jupiter.api.Assertions.*;

import static com.fathzer.odvpn.utils.TarGzUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TarGzUtilsTest {
    private static final String FOLDER_NAME = "folder";
    private static final String TEST_FILE = "/test.tar.gz";
    private static final String EMPTY_FILE = "empty.txt";
    private static final String FOLDER_FILE = FOLDER_NAME + "/file.txt";
    private static final String EXPECTED_CONTENT = "blabla";

    @Test
    void testContainsEmptyFile() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEST_FILE)) {
            assertNotNull(is, "Test file not found");
            assertTrue(contains(is, EMPTY_FILE, false), 
                "The tar.gz should contain " + EMPTY_FILE);
        }
    }

    @Test
    void testContainsFileInFolder() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEST_FILE)) {
            assertNotNull(is, "Test file not found");
            assertTrue(contains(is, FOLDER_FILE, false), 
                "The tar.gz should contain " + FOLDER_FILE);
        }
    }
    
    @Test
    void testContainsFolder() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEST_FILE)) {
            assertNotNull(is, "Test file not found");
            assertTrue(contains(is, FOLDER_NAME, true), 
                    "The tar.gz should contain folder");
        }
        try (InputStream is = getClass().getResourceAsStream(TEST_FILE)) {
            assertTrue(contains(is, FOLDER_NAME + "/", true), 
                "The tar.gz should contain folder");
        }
    }
    
    @Test
    void testFileContent() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEST_FILE)) {
            // Test reading file content using getEntryStream
            try (InputStream fileStream = getEntryStream(is, FOLDER_FILE, false)) {
                assertNotNull(fileStream, "File should be found");
                String content = new String(fileStream.readAllBytes(), StandardCharsets.UTF_8);
                assertEquals(EXPECTED_CONTENT, content.trim(), "File content should match");
            }
        }
    }
}
