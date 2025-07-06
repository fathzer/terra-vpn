package com.fathzer.odvpn;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class VPNConfigValidator {
    private VPNConfigValidator() {}

    /**
     * Checks if the specified tar.gz file contains a pki/issued directory with the given certificate file.
     *
     * @param tarGzFile The tar.gz file to check
     * @param targetPath The path of the file to look for in the tar.gz file
     * @return true if the file exists in the tar.gz file, false otherwise
     * @throws IOException if there's an error reading the tar.gz file
     */
    /**
     * Checks if the specified tar.gz input stream contains a file with the given path.
     * @param tarGzStream The input stream of the tar.gz file to check
     * @param targetPath The path of the file to look for in the tar.gz file
     * @return true if the file exists in the tar.gz file, false otherwise
     * @throws IOException if there's an error reading the tar.gz file
     * @throws IllegalArgumentException if targetPath is null or empty
     */
    public static boolean contains(InputStream tarGzStream, String targetPath, boolean directory) throws IOException {
        if (targetPath == null || targetPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Target path cannot be null or empty");
        }
        targetPath = normalizePath(targetPath, directory);
        try (GZIPInputStream gzis = new GZIPInputStream(tarGzStream);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                String entryName = normalizePath(entry.getName(), entry.isDirectory());
                System.out.println("Entry: " + entryName);
                if (targetPath.equals(entryName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String normalizePath(String entryName, boolean directory) {
        entryName = entryName.replace('\\', '/');
        if (!entryName.startsWith("./")) {
            entryName = "./" + entryName;
        }
        if (!directory && entryName.endsWith("/")) {
            entryName = entryName.substring(0, entryName.length() - 1);
        } else if (directory && !entryName.endsWith("/")) {
            entryName += "/";
        }
        return entryName;
    }
}
