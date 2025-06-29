package com.fathzer.odvpn.ws.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fathzer.odvpn.ssh.KeyGenerator;

@Configuration
public class SettingsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SettingsConfiguration.class);

    @Bean
    public ValidatedSettings validatedSettings() {
        final Path dataPath = Path.of(System.getProperty("data.path", "data"));
        
        final Path sshPrivateKeyPath = Path.of(System.getProperty("privateKey.path", dataPath.resolve(".ssh/id_rsa").toString()));
        logger.info("Starting On Demand VPN web services with data.path: {} and privateKey.path: {}", dataPath, sshPrivateKeyPath);
        if (Files.isRegularFile(dataPath)) {
            throw new IllegalStateException("Data path is not a directory");
        } else if (!Files.exists(dataPath)) {
            try {
                logger.info("Creating data directory: {}", dataPath.toAbsolutePath());
                Files.createDirectories(dataPath);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create data path");
            }
        }

        if (Files.isRegularFile(sshPrivateKeyPath)) {
            return new ValidatedSettings(dataPath.toString(), sshPrivateKeyPath.toString());
        } else if (Files.exists(sshPrivateKeyPath) || System.getProperty("privateKey.path") != null) {
            throw new IllegalStateException(String.format("SSH private key path %s is not a file", sshPrivateKeyPath.toAbsolutePath()));
        } else {
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
            return new ValidatedSettings(dataPath.toString(), sshPrivateKeyPath.toString());
        }
    }
}
