package com.fathzer.odvpn;

import static org.junit.jupiter.api.Assertions.*;

import static com.fathzer.odvpn.VPNConfigValidator.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

class VPNConfigValidatorTest {
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
            assertTrue(contains(is, FOLDER_NAME + "/", true), 
                "The tar.gz should contain folder");
        }
    }
    
    @Test
    void testFileContent() throws IOException {
        try (InputStream is = getClass().getResourceAsStream(TEST_FILE);
             GZIPInputStream gzis = new GZIPInputStream(is);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            
            TarArchiveEntry entry;
            boolean fileFound = false;
            String searched = normalizePath(FOLDER_FILE, false);
            
            while ((entry = tais.getNextEntry()) != null) {
                String entryName = normalizePath(entry.getName(), entry.isDirectory());
                if (searched.equals(entryName)) {
                    fileFound = true;
                    byte[] content = new byte[(int) entry.getSize()];
                    tais.read(content);
                    String actualContent = new String(content, StandardCharsets.UTF_8);
                    assertEquals(EXPECTED_CONTENT, actualContent.trim(), 
                        "File content does not match expected value");
                    break;
                }
            }
            
            assertTrue(fileFound, FOLDER_FILE + " not found in the archive");
        }
    }
}
