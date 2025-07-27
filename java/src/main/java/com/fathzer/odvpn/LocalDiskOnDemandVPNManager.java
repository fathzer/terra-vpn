package com.fathzer.odvpn;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.json.InstanceParametersParser;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.VPNRepositorySettings;

public class LocalDiskOnDemandVPNManager extends AbstractOnDemandVPNManager {
    private final Path root;
    private final Path sshPrivateKey;

    public LocalDiskOnDemandVPNManager(VPNRepositorySettings settings, String id, InstanceParameters config) {
        super(id, config);
        if (settings == null) {
            throw new IllegalArgumentException("settings cannot be null");
        }

        this.sshPrivateKey = settings.privateKeyPath();
        if (!Files.isRegularFile(sshPrivateKey)) {
            throw new IllegalArgumentException("SSH private key cannot be null or not a file");
        }
        this.root = settings.dataPath().resolve(id);
    }

    public LocalDiskOnDemandVPNManager(VPNRepositorySettings settings, String id) throws IOException {
        this(settings, id, InstanceParametersParser.parse(settings.dataPath().resolve(id).resolve("config.json")));
    }

    private Path getVpsInfoPath() {
        return root.resolve("vps.json");
    }

    @Override
    protected VPSInfo getLocalVPSInfo() throws IOException {
        final Path path = getVpsInfoPath();
        if (!Files.exists(path)) {
            return null;
        }
        return new ObjectMapper().readValue(path.toFile(), VPSInfo.class);
    }

    @Override
    protected void saveLocalVPSInfo(VPSInfo vpsInfo) throws IOException {
        final Path path = getVpsInfoPath();
        Files.createDirectories(path.getParent());
        new ObjectMapper().writeValue(path.toFile(), vpsInfo);
    }

    @Override
    protected void deleteLocalVPSInfo() throws IOException {
        Files.delete(getVpsInfoPath());
    }

    @Override
    protected boolean exists() throws IOException {
        return Files.exists(root);
    }

    /**
     * Saves the configuration in the persistent storage.
     * @param openVpnConfigStream the InputStream to the OpenVPN configuration file (or null if no OpenVPN configuration is provided)
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void save(InputStream openVpnConfigStream) throws IOException {
        Files.createDirectories(root);
        InstanceParametersParser.write(root.resolve("config.json"), config);
        if (openVpnConfigStream != null) {
            Files.copy(openVpnConfigStream, root.resolve(OpenVPNManager.OPENVPN_TAR_GZ), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    @Override
    protected void erase() throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    protected Path getOpenVPNConfigPath() {
        return root.resolve(OpenVPNManager.OPENVPN_TAR_GZ);
    }

    @Override
    protected Path getSSHPrivateKeyPath() {
        return sshPrivateKey;
    }
}
