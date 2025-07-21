package com.fathzer.odvpn;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.fathzer.odvpn.OpenVPNManager.User;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.ssh.Ssh;
import com.fathzer.odvpn.utils.DnsUpdateAwaiter;

public abstract class AbstractOnDemandVPNManager {

    public static class ConfigurationException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

		public ConfigurationException(List<String> errors) {
            super("Configuration errors: " + errors);
        }
    }

    public static class DetailedStatus {
        private boolean ready;
        private boolean openvpnConfigured;
        private boolean ddnsUpdated;
        private boolean sshReady;
        private boolean vpsCreated;
        private boolean openvpnServerStarted;

        private DetailedStatus() {
            // Do nothing
        }

        private void setReady(boolean ready) {
            this.ready = ready;
            this.openvpnConfigured = ready;
            this.ddnsUpdated = ready;
            this.sshReady = ready;
            this.vpsCreated = ready;
            this.openvpnServerStarted = ready;
        }

        public boolean isReady() {
            return ready;
        }
        public boolean isOpenvpnServerStarted() {
            return openvpnServerStarted;
        }
        public boolean isOpenvpnConfigured() {
            return openvpnConfigured;
        }
        public boolean isDdnsUpdated() {
            return ddnsUpdated;
        }
        public boolean isSshReady() {
            return sshReady;
        }
        public boolean isVpsCreated() {
            return vpsCreated;
        }
    }

    public record VPSInfo(String id, String ip) {}

    protected final String id;
    protected final InstanceParameters config;
    private DetailedStatus status;

    protected AbstractOnDemandVPNManager(String id, InstanceParameters config) {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("Invalid VPN ID: " + id+" (must start with a letter or a number and contain only letters, numbers, dots, underscores and hyphens)");
        }
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        this.id = id;
        this.config = config;
    }

    /** Checks if the given id is valid
     * <br>A valid id must start with a letter or a number and contain only letters, numbers, dots, underscores and hyphens
     * @param id the id to check
     * @return true if the id is valid, false otherwise
     */
    public static boolean isValidId(String id) {
        return id != null && id.matches("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");
    }

    /** Gets the VPN id
     * @return a String
     */
    public String id() {
        return id;
    }

    /** Gets the VPN configuration
     * @return the VPN configuration
     */
    public InstanceParameters settings() {
        return config;
    }

    /** Checks if the VPN server is running (by checking the VPS provider)
     * @return true if the VPN server is running, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public boolean isServerRunning() throws IOException {
        VPSInfo vpsInfo = getLocalVPSInfo();
        return vpsInfo!=null && config.vps().exists(vpsInfo.id());
    }

    /**
     * Gets the VPS information (id and ip address) stored in the persistent storage.
     * @return the VPS information or null if the VPS does not exist in persistent storage
     * @throws IOException if an I/O error occurs
     */
    protected abstract VPSInfo getLocalVPSInfo() throws IOException;

    /**
     * Saves the VPS information in the persistent storage.
     * @param vpsInfo the VPS information
     * @throws IOException if an I/O error occurs
     */
    protected abstract void saveLocalVPSInfo(VPSInfo vpsInfo) throws IOException;

    /**
     * Deletes the VPS information in the persistent storage.
     * @throws IOException if an I/O error occurs
     */
    protected abstract void deleteLocalVPSInfo() throws IOException;

    /**
     * Checks if the VPS exists in the persistent storage.
     * @return true if the VPS exists, false otherwise
     * @throws IOException if an I/O error occurs
     */
    protected abstract boolean exists() throws IOException;

    /**
     * Saves the configuration in the persistent storage.
     * @param openVpnConfigPath the path to the OpenVPN configuration file (or null if no OpenVPN configuration is provided)
     * @throws IOException if an I/O error occurs
     */
    protected abstract void save(Path openVpnConfigPath) throws IOException;

    /**
     * Deletes the configuration in the persistent storage.
     * @throws IOException if an I/O error occurs
     */
    protected abstract void erase() throws IOException;

    /**
     * Initializes the configuration.
     * @param openVpnConfigPath the path to the OpenVPN configuration file (or null if no OpenVPN configuration is provided)
     * @param force if true, the configuration file will be overwritten if it already exists and the server is not running
     * @throws IOException if an I/O error occurs
     * @throws ConfigurationException if the configuration is invalid
     * @throws IllegalStateException if the configuration file already exists and force is false or if the server is running.
     */
    public void init(Path openVpnConfigPath, boolean force) throws IOException {
        if (exists() && !force) {
            throw new IllegalStateException("Configuration file already exists");
        } else {
            if (isServerRunning()) {
                throw new IllegalStateException("Can't change configuration of a running server");
            } else {
                checkConfiguration(openVpnConfigPath);
                save(openVpnConfigPath);
            }
        }
    }

    /**
     * Gets the path to the local OpenVPN configuration file.
     * @return the path to the OpenVPN configuration file
     */
    protected abstract Path getOpenVPNConfigPath();    
    /**
     * Gets the path to the SSH private key.
     * @return the path to the SSH private key
     */
    protected abstract Path getSSHPrivateKeyPath();

    private void checkConfiguration(Path openVpnConfigPath) throws IOException {
        // First check the vps configuration
        final List<String> errors = new LinkedList<>();
        errors.addAll(config.vps().checkConfiguration());
        // Then check the ddns configuration
        errors.addAll(config.ddns().checkConfiguration(config.vpn().hostname()));
        // Finally, check the openVpnConfiguration
        if (openVpnConfigPath == null) {
            openVpnConfigPath = getOpenVPNConfigPath();
        }
        if (Files.exists(openVpnConfigPath)) {
            errors.addAll(VPNConfigValidator.check(config.vpn(), openVpnConfigPath));
        }
        if (!errors.isEmpty()) {
            throw new ConfigurationException(errors);
        }
    }

    /** Starts the VPN server
     * @param progressListener the progress listener to notify of the progress
     * @throws IOException if an I/O error occurs
     */
    public void start(StartProgressListener progressListener) throws IOException {
        final AtomicBoolean ddnsUpdated = new AtomicBoolean(false);
        // Create the VPS if it does not exist
        this.status = new DetailedStatus();
        final String ip;
        if (!isServerRunning()) {
            DNSUpdateProgressListener listener = new DNSUpdateProgressListener(ddnsUpdated, progressListener);
            VPSProvider.VPSState vpsState = config.vps().createVPS(config.vpn(), listener);
            ip = vpsState.ip();
            saveLocalVPSInfo(new VPSInfo(vpsState.id(), ip));
        } else {
            ip = getLocalVPSInfo().ip();
        }
        this.status.vpsCreated = true;

        // Update DDNS if needed
        if (!ddnsUpdated.get()) {
            updateDDNS(ip, progressListener);
        }
        this.status.ddnsUpdated = true;

        // Wait for ssh connection is available
        final String keyPath = getSSHPrivateKeyPath().toAbsolutePath().toString();
        final String sshUser = config.vps().getSSHUser();
        Ssh.Builder builder = new Ssh.Builder(ip, keyPath).user(sshUser).maxTryCount(24);
        progressListener.waitingSSHConnection(ip);
        try (Ssh ssh = builder.build()) {
            // Do nothing, we just connect to check if the connection is available
        }
        this.status.sshReady = true;

        // Do openvpn configuration or restore it
        try (OpenVPNManager openVPNConfigManager = new OpenVPNManager(ip, sshUser, getSSHPrivateKeyPath())) {
            if (Files.exists(getOpenVPNConfigPath())) {
                progressListener.restoringOpenVPNConfiguration();
                openVPNConfigManager.restore(getOpenVPNConfigPath());
            } else {
                progressListener.creatingOpenVPNConfiguration();
                openVPNConfigManager.initRemote(config);
                openVPNConfigManager.save(getOpenVPNConfigPath());
            }
            this.status.openvpnConfigured = true;
            // Start the server
            progressListener.startingOpenVPNServer();
            openVPNConfigManager.start(config);
            this.status.openvpnServerStarted = true;
        }
        String hostName = config.vpn().hostname();
        progressListener.waitingDNSPropagation();
        new DnsUpdateAwaiter(60, 5000).waitFor(hostName, ip);
        this.status.ready = true;
        progressListener.ready();
    }

    private void updateDDNS(String ip, StartProgressListener progressListener) throws IOException {
        progressListener.updatingDDNS(config.vpn().hostname(), ip);
        config.ddns().updateDns(config.vpn().hostname(), ip);
    }

    private class DNSUpdateProgressListener implements Consumer<VPSProvider.VPSState> {
        private final AtomicBoolean ddnsUpdated;
        private final StartProgressListener chained;
        
        private DNSUpdateProgressListener(AtomicBoolean ddnsUpdated, StartProgressListener chained) {
            this.ddnsUpdated = ddnsUpdated;
            this.chained = chained;
        }

        @Override
        public void accept(VPSProvider.VPSState state) {
            chained.creatingVPS(state);
            try {
                if (!ddnsUpdated.get() && state.status().equals(VPSProvider.Status.IP_READY)) {
                    ddnsUpdated.set(true);
                    updateDDNS(state.ip(), this.chained);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** Stops the VPN server
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if the VPS is not running
     */
    public void stop() throws IOException {
        VPSInfo vpsInfo = getLocalVPSInfo();
        if (vpsInfo == null) {
            throw new IllegalStateException("VPS is not running");
        }
        config.vps().deleteVPS(vpsInfo.id());
        deleteLocalVPSInfo();
    }

    /** Deletes the VPN configuration
     * @throws IOException if an I/O error occurs
     */
    public void delete(boolean force) throws IOException {
        if (isServerRunning() && !force) {
            throw new IllegalStateException("Can't delete a running server, please stop it first");
        } else if (exists()) {
            erase();
        }
    }

    public DetailedStatus getStatus() throws IOException {
        if (status == null) {
            // TODO Maybe the status could be refined to be more accurate (is docker running, etc ...)
            // Currently it simply checks that the VPS is running
            this.status = new DetailedStatus();
            this.status.setReady(isServerRunning());
        }
        return status;
    }

    protected OpenVPNManager getOpenVPNManager() throws IOException {
        return new OpenVPNManager(getLocalVPSInfo().ip(), config.vps().getSSHUser(), getSSHPrivateKeyPath());
    }
    
    public List<User> getUsers() throws IOException {
        return getOpenVPNManager().getUsers();
    }

    public void addUser(String name) throws IOException {
        final OpenVPNManager openVPNManager = getOpenVPNManager();
        openVPNManager.addUser(name);
        openVPNManager.save(getOpenVPNConfigPath());
    }

    public void deleteUser(String name) throws IOException {
        final OpenVPNManager openVPNManager = getOpenVPNManager();
        openVPNManager.deleteUser(name);
        openVPNManager.save(getOpenVPNConfigPath());
    }

    public byte[] getUserConfig(String name) throws IOException {
        return String.join("\n", getOpenVPNManager().getUserConfigurationFile(name)).getBytes(StandardCharsets.UTF_8);
    }
}
