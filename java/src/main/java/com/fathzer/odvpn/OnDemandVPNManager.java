package com.fathzer.odvpn;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.json.InstanceParametersParser;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.ssh.Ssh;

public class OnDemandVPNManager {
    private static final Logger logger = LoggerFactory.getLogger(OnDemandVPNManager.class);

    public static class ConfigurationException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

		public ConfigurationException(List<String> errors) {
            super("Configuration errors: " + errors);
        }
    }

    private final InstanceParameters config;
    private final Path root;
    private final Path sshPrivateKey;

    public OnDemandVPNManager(InstanceParameters config, Path root, Path sshPrivateKey) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (root == null) {
            throw new IllegalArgumentException("Output directory cannot be null");
        }
        if (!isValidId(root.getFileName().toString())) {
            throw new IllegalArgumentException("Invalid VPN ID: " + root.getFileName()+" (must start with a letter or a number and contain only letters, numbers, dots, underscores and hyphens)");
        }
        if (sshPrivateKey == null || !Files.isRegularFile(sshPrivateKey)) {
            throw new IllegalArgumentException("SSH private key cannot be null or not a file");
        }
        this.config = config;
        this.root = root;
        this.sshPrivateKey = sshPrivateKey;
    }

    public OnDemandVPNManager(Path root, Path sshPrivateKey) throws IOException {
        this(InstanceParametersParser.parse(root.resolve("config.json")), root, sshPrivateKey);
    }

    public static boolean isValidId(String id) {
        return id != null && id.matches("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");
    }

    public String id() {
        return root.getFileName().toString();
    }

    public InstanceParameters settings() {
        return config;
    }

    public boolean isServerRunning() throws IOException {
        final Path path = root.resolve("vps.json");
        if (!Files.exists(path)) {
            return false;
        }
        return config.vps().provider().exists(config, getVPSInfo().get("id"));
    }

    private Map<String, String> getVPSInfo() throws IOException {
        final Path path = root.resolve("vps.json");
        if (!Files.exists(path)) {
            return Map.of();
        }
        return new ObjectMapper().readValue(path.toFile(), new TypeReference<Map<String, String>>() {});
    }

    private void saveVPSInfo(String id, String ip) throws IOException {
        final Path path = root.resolve("vps.json");
        Files.createDirectories(path.getParent());
        new ObjectMapper().writeValue(path.toFile(), Map.of("id", id, "ip", ip));
    }

    private void checkConfiguration() throws IOException {
        // First check the vps configuration
        List<String> errors = config.vps().provider().checkConfiguration(config.vps());
        // TODO check DDNS and openvpn configuration
        if (!errors.isEmpty()) {
            throw new ConfigurationException(errors);
        }
    }

    /**
     * Builds the configuration.
     * @param force if true, the configuration file will be overwritten if it already exists and the server is not running
     * @throws IOException if an I/O error occurs
     * @throws ConfigurationException if the configuration is invalid
     * @throws IllegalStateException if the configuration file already exists and force is false or if the server is running.
     */
    public void init(Path openVpnConfigPath, boolean force) throws IOException {
        checkConfiguration();
        final Path configPath = root.resolve("config.json");
        if (Files.exists(configPath) && !force) {
            throw new IllegalStateException("Configuration file already exists: " + configPath.toAbsolutePath());
        } else {
            if (isServerRunning()) {
                throw new IllegalStateException("Can't change configuration of a running server");
            } else {
                Files.createDirectories(configPath.getParent());
                InstanceParametersParser.write(configPath, config);
                if (openVpnConfigPath != null) {
                    Files.copy(openVpnConfigPath, root.resolve(OpenVPNManager.OPENVPN_TAR_GZ), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Deletes the configuration directory and all its contents.
     * @throws IOException if an I/O error occurs
     */
    public void delete() throws IOException {
        if (isServerRunning()) {
            throw new IllegalStateException("Can't delete a running server, please stop it first");
        }
        if (Files.exists(root)) {
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
    }

    private class StartProgressListener implements Consumer<VPSProvider.VPSState> {
        private final AtomicBoolean ddnsUpdated;
        
        public StartProgressListener(AtomicBoolean ddnsUpdated) {
            this.ddnsUpdated = ddnsUpdated;
        }
        
        @Override
        public void accept(VPSProvider.VPSState state) {
            try {
                logger.info("VPS state: {}", state);
                if (!ddnsUpdated.get() && state.status().equals(VPSProvider.Status.IP_READY)) {
                    ddnsUpdated.set(true);
                    updateDDNS(state.ip());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void updateDDNS(String ip) throws IOException {
        try {
            logger.info("Updating DDNS for {} with IP {}", config.hostName(), ip);
            config.ddns().provider().updateDns(config.ddns().config(), config.hostName(), ip);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
    }

    public void start() throws IOException, InterruptedException {
        final AtomicBoolean ddnsUpdated = new AtomicBoolean(false);
        // Create the VPS if it does not exist
        final String ip;
        if (!isServerRunning()) {
            StartProgressListener progressListener = new StartProgressListener(ddnsUpdated);
            VPSProvider.VPSState vpsState = config.vps().provider().createVPS(config, progressListener);
            ip = vpsState.ip();
            saveVPSInfo(vpsState.id(), ip);
        } else {
            ip = getVPSInfo().get("ip");
        }

        // Update DDNS if needed
        if (!ddnsUpdated.get()) {
            updateDDNS(ip);
        }

        // Wait for ssh connection is available
        final String keyPath = sshPrivateKey.toAbsolutePath().toString();
        final String sshUser = VPSProvider.getSSHUser(config.vps());
        Ssh.Builder builder = new Ssh.Builder(ip, keyPath).user(sshUser).maxTryCount(24);
        try (Ssh ssh = builder.build()) {
            logger.info("SSH connection is available");
        }

        // Do openvpn configuration or restore it
        try (OpenVPNManager openVPNConfigManager = new OpenVPNManager(root, ip, sshUser, sshPrivateKey)) {
            if (openVPNConfigManager.hasBackup()) {
                logger.info("Restoring openvpn configuration");
                openVPNConfigManager.restore();
            } else {
                openVPNConfigManager.initRemote(config);
                logger.info("Saving openvpn configuration");
                openVPNConfigManager.save();
            }
            // Start the server
            //TODO Not sure it is a good idea to make this check here. One could want to add users before the DNS propagates...
            openVPNConfigManager.start(config);
            logger.info("Openvpn server is ready");
        }
        waitForDNS(config, ip);        
    }

    static void doSSHCommand(Ssh ssh, String command) throws IOException {
        logger.debug("Executing command: {}", command);
//        int code = ssh.exec(command, new LoggerOutputStream(logger, LogLevel.DEBUG), new LoggerOutputStream(logger, LogLevel.DEBUG));
        int code = ssh.exec(command, OutputStream.nullOutputStream(), OutputStream.nullOutputStream());
        logger.debug("Command finished with exit code: {}", code);
        if (code != 0) {
            throw new IOException("Command " + command + " failed with exit code " + code);
        }
    }

    private void waitForDNS(InstanceParameters config, String ip) throws IOException, InterruptedException {
        final int maxAttempts = 60; // 60 attempts * 5 seconds = 5 minutes max
        final long delayMs = 5000; // 5 seconds between attempts
        
        String hostName = config.hostName();
        logger.info("Waiting for DNS propagation of {} to point to {}", hostName, ip);
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                InetAddress address = InetAddress.getByName(hostName);
                String resolvedIp = address.getHostAddress();
                
                if (ip.equals(resolvedIp)) {
                    logger.info("DNS successfully resolved to {}", ip);
                    return;
                }
                logger.debug("DNS resolution attempt {}/{}: {} resolves to {}, expected {}",  attempt, maxAttempts, hostName, resolvedIp, ip);
            } catch (Exception e) {
                logger.debug("DNS resolution attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
            }
            if (attempt < maxAttempts) {
                Thread.sleep(delayMs);
            }
        }
        throw new IOException(String.format("Timeout waiting for DNS propagation of %s to %s", hostName, ip));
    }
}
