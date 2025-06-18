package com.fathzer.terravpn.ssh;

import java.util.ArrayList;
import java.util.List;

public class SSHUtils {
    private SSHUtils() {
    }

    public static void createKeyPair() {
        //TODO
    }

    /**
     * Format a key to be used in ssh
     * @param key the key in a single line (like in a json file)
     * @return the formatted key, ready to be used in ssh
     */
    public static List<String> formatKey(String key) {
        if (key.charAt(0) != '-' || key.charAt(key.length() - 1) != '-') {
            throw new IllegalArgumentException("Invalid key format");
        }
        // skip leading '-''
        int endOfHeader = 1;
        while (endOfHeader < key.length() && key.charAt(endOfHeader) == '-') {
            endOfHeader++;
        }
        // skip header content
        endOfHeader = key.indexOf('-', endOfHeader);
        if (endOfHeader == -1) {
            throw new IllegalArgumentException("Invalid key format");
        }
        // skip trailing '-'
        while (endOfHeader < key.length() && key.charAt(endOfHeader) == '-') {
            endOfHeader++;
        }
        if (endOfHeader == key.length()) {
            throw new IllegalArgumentException("Invalid key format");
        }

        final List<String> result = new ArrayList<>();
        result.add(key.substring(0, endOfHeader));

        final int endOfContent = key.indexOf('-', endOfHeader);
        if (endOfContent == -1) {
            throw new IllegalArgumentException("Invalid key format");
        }
        String content = key.substring(endOfHeader, endOfContent).replaceAll("[^A-Za-z0-9+/=]", "");
        for (int i = 0; i < content.length(); i += 64) {
            int end = Math.min(i + 64, content.length());
            result.add(content.substring(i, end));
        }
        result.add(key.substring(endOfContent));
        return result;
    }
}
