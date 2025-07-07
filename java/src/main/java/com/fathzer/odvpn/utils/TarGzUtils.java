package com.fathzer.odvpn.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class TarGzUtils {
    private TarGzUtils() {}

    /**
     * Checks if the specified tar.gz input stream contains a file with the given path.
     * <p><b>Note:</b> This method will close the input stream before returning.</p>
     * @param tarGzStream The input stream of the tar.gz file to check (will be closed by this method)
     * @param targetPath The path of the file to look for in the tar.gz file
     * @param directory whether the target path refers to a directory
     * @return true if the file exists in the tar.gz file, false otherwise
     * @throws IOException if there's an error reading the tar.gz file
     * @throws IllegalArgumentException if targetPath is null or empty
     */
    public static boolean contains(InputStream tarGzStream, String targetPath, boolean directory) throws IOException {
        try (InputStream is = getEntryStream(tarGzStream, targetPath, directory)) {
            return is != null;
        }
    }

    /**
     * Returns an input stream for the specified entry in the tar.gz file.
     * <p><b>Important:</b> The returned stream (if not null) must be closed by the caller.
     * The input stream ({@code tarGzStream}) will be closed when the returned stream is closed.
     * If no matching entry is found, the input stream will be closed before this method returns.</p>
     * @param tarGzStream The input stream of the tar.gz file (will be managed by the returned stream)
     * @param targetPath The path of the entry to look for in the tar.gz file
     * @param directory whether the target path refers to a directory
     * @return An input stream for the specified entry, or null if the entry is not found. The caller is responsible for closing the returned stream.
     * @throws IOException if there's an error reading the tar.gz file
     * @throws IllegalArgumentException if targetPath is null or empty
     */
    public static InputStream getEntryStream(InputStream tarGzStream, String targetPath, boolean directory) throws IOException {
        if (targetPath == null || targetPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Target path cannot be null or empty");
        }
        targetPath = normalizePath(targetPath, directory);
        
        InputStream result = null;
        final TarArchiveInputStream tais = new TarArchiveInputStream(new GZIPInputStream(tarGzStream));
        try {
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                String entryName = normalizePath(entry.getName(), entry.isDirectory());
                if (targetPath.equals(entryName)) {
                    // Retourne un InputStream qui lira à partir de la position actuelle dans le tar
                    result = tais;
                    break;
                }
            }
        } finally {
            if (result == null) {
                tais.close();
            }
        }
        return result;
    }

    /**
     * Normalizes the path of an entry in the tar.gz file.
     * @param entryName The path of the entry to normalize
     * @param directory true if the entry is a directory, false otherwise
     * @return The normalized path of the entry
     */
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
