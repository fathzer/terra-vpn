package com.fathzer.terravpn;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class SSHUtilsTest {
    private static final String BASE64_PATTERN = "^[A-Za-z0-9+/=]+$";

    @Test
    void testBasicFormatKey() {
        String header = "--- P1 --";
        String footer = "--P3-";
        String content = "kjkmmk";
        String key = header+content+footer;
        List<String> formattedKey = SSHUtils.formatKey(key);
        checkFormattedKey(header, footer, content, formattedKey);
    }

    @Test
    void testFormatKey() {
        String header = "-----BEGIN OPENSSH PRIVATE KEY-----";
        String footer = "-----END OPENSSH PRIVATE KEY-----";
        String content = "b3BlbnNzaC1rZXktdjEAAAACFwAAAAdzc2gtcn5dVk+c82LhZpjsZejUzxAELliJX9i9fLiSBMkFa8+Lo3bIRX0jwjs8qgfr\nfFn";
        String key = header+content+footer;
        List<String> formattedKey = SSHUtils.formatKey(key);
        checkFormattedKey(header, footer, content, formattedKey);

        // Test with different headers and footers
        header = "------ BEGIN PRIVATE KEY ------";
        footer = "----- END PRIVATE KEY -----";
        key = header+content+footer;
        formattedKey = SSHUtils.formatKey(key);
        checkFormattedKey(header, footer, content, formattedKey);
        
    }

    private void checkFormattedKey(String header, String footer, String content, List<String> formattedKey) {
        assertEquals(header, formattedKey.get(0));
        assertEquals(footer, formattedKey.get(formattedKey.size()-1));
        formattedKey.remove(0);
        formattedKey.remove(formattedKey.size()-1);
        String storedKey = formattedKey.stream().collect(Collectors.joining(""));
        assertEquals(content.replaceAll("[^A-Za-z0-9+/=]", ""), storedKey);
        // Check there's only base64 characters
        formattedKey.forEach(e -> assertTrue(e.matches(BASE64_PATTERN)));
        // Check last line is not empty and <= 64 characters
        String lastLine = formattedKey.get(formattedKey.size()-1);
        assertTrue(!lastLine.isEmpty() && lastLine.length() <= 64 && lastLine.matches(BASE64_PATTERN));
        // Check other lines not empty and 64 characters long and base64
        formattedKey.remove(formattedKey.size()-1);
        formattedKey.forEach(e -> assertTrue(!e.isEmpty() && e.length() == 64 && e.matches(BASE64_PATTERN)));
    }
}
