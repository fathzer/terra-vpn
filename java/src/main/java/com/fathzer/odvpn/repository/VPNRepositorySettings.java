package com.fathzer.odvpn.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.ssh.KeyGenerator;

public record VPNRepositorySettings(Path dataPath, Path privateKeyPath) {
    private static final Logger logger = LoggerFactory.getLogger(VPNRepositorySettings.class);

    public static VPNRepositorySettings fromEnvironment(boolean autoCreateKeyPair) {
        final Path dataPath = getPath("ODVPN_DATA_DIR", "data.dir", "data");
        final Path sshPrivateKeyPath = getPath("ODVPN_SSH_KEY_PATH", "privateKey.path", ".ssh/id_rsa");
        logger.info("On Demand VPN launched with data.dir: {} and privateKey.path: {}", dataPath, sshPrivateKeyPath);
        if (Files.isRegularFile(dataPath)) {
            throw new IllegalStateException("Data directory is not a directory");
        } else if (!Files.exists(dataPath)) {
            try {
                logger.info("Creating data directory: {}", dataPath.toAbsolutePath());
                Files.createDirectories(dataPath);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create data directory");
            }
        }

        if (Files.isRegularFile(sshPrivateKeyPath)) {
            return new VPNRepositorySettings(dataPath, sshPrivateKeyPath);
        } else if (Files.exists(sshPrivateKeyPath)) {
            throw new IllegalStateException(String.format("SSH private key path %s is not a file", sshPrivateKeyPath.toAbsolutePath()));
        } else if (autoCreateKeyPair) {
            // File does not exists but its path is the default one => create it
            logger.info("SSH private key not found, creating a key pair in: {}", sshPrivateKeyPath.getParent().toAbsolutePath());
            try {
                Files.createDirectories(sshPrivateKeyPath.getParent());
                final Path sshPublicKeyPath = sshPrivateKeyPath.resolveSibling("id_rsa.pub");
                KeyGenerator.createRSAKeyPair(2048, sshPrivateKeyPath, sshPublicKeyPath);
                logger.info("SSH key pair created. Don't forget to register the public key ({}) in your VPS provider's console", sshPublicKeyPath.toAbsolutePath());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create SSH private key", e);
            }
            return new VPNRepositorySettings(dataPath, sshPrivateKeyPath);
        } else {
            throw new IllegalStateException(String.format("SSH private key file %s not found", sshPrivateKeyPath.toAbsolutePath()));
        }
    }

    private static Path getPath(String envVar, String property, String defaultValue) {
        String path = System.getenv(envVar);
        if (path == null) {
            path = System.getProperty(property, defaultValue);
        }
        return Path.of(path);
    }
}
