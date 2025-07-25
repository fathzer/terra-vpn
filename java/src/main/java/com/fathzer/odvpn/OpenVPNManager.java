package com.fathzer.odvpn;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.ssh.Ssh;
import com.fathzer.odvpn.utils.ListOutputStream;
import com.fathzer.odvpn.utils.IOLambdas.IOSupplier;

/**
 * Manages the openvpn server on the remote VPS
 */
public class OpenVPNManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OpenVPNManager.class);
    /** The OpenVPN image to use */
    static final String OPENVPN_IMAGE = "kylemanna/openvpn";
    /** The OpenVPN configuration folder on the VPS */
    static final String OPENVPN_VPS_FOLDER = "/etc/openvpn";

    static final String OPENVPN_TAR_GZ = "openvpn.tar.gz";

    private final Ssh ssh;

    public static class UnknownUserException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public UnknownUserException(String name) {
            super(name);
        }
    }

    public static class UserAlreadyExistsException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        public UserAlreadyExistsException(String name) {
            super(name);
        }
    }

    /**
     * Creates a new OpenVPNManager
     * @param address the address of the server
     * @param sshUser the SSH user (e.g. root)
     * @param sshPrivateKey the path to the SSH private key
     * @throws IOException if an error occurs
     */
    OpenVPNManager(String address, String sshUser, Path sshPrivateKey) throws IOException {
        this(() -> new Ssh.Builder(address, sshPrivateKey.toAbsolutePath().toString()).user(sshUser).build());
    }

    protected OpenVPNManager(IOSupplier<Ssh> sshSupplier) throws IOException {
        this.ssh = sshSupplier.get();
    }

    /** Saves the remote openvpn backup config to the local file
     * @throws IOException if an error occurs
    */
    public void save(Path localFile) throws IOException {
        doSSHCommand(ssh, "sudo tar -czf " + OPENVPN_TAR_GZ + " -C "+OPENVPN_VPS_FOLDER+" .");
        ssh.download(OPENVPN_TAR_GZ, localFile.toString());
    }

    /** Uploads and apply the local openvpn backup config to the remote server
     * <br>Note: does not start the server
     * @throws IOException if an error occurs
    */
    public void restore(Path localFile) throws IOException {
        // Ensure the remote file does not exist (to avoid permission issues as it is created in sudo mode)
        doSSHCommand(ssh, "sudo rm -f " + OPENVPN_TAR_GZ);
        ssh.upload(localFile.toString(), OPENVPN_TAR_GZ);
        final List<String> commands = Arrays.asList(
			"#!/bin/bash",
			"set -e",
			"sudo rm -rf "+ OPENVPN_VPS_FOLDER,
            "sudo mkdir -p "+OPENVPN_VPS_FOLDER,
			"sudo tar -xzf " + OPENVPN_TAR_GZ+ " -C "+OPENVPN_VPS_FOLDER,
			"rm "+OPENVPN_TAR_GZ
    		);
        ssh.exec(commands, System.out, System.err);
    }

    /** Initializes the remote openvpn config
     * <br>Note: does not start the server nor perform any backup
     * @throws IOException if an error occurs
    */
    public void initRemote(InstanceParameters config) throws IOException {
        // Erase any previous configuration (if any)
        doSSHCommand(ssh, "sudo rm -rf " + OPENVPN_VPS_FOLDER);
        String initOpenVPNCommand = getInitOpenVPNCommand(config);
        doSSHCommand(ssh, initOpenVPNCommand);
        final String initPKICommand = "echo 'yes' | docker run -v " + OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OPENVPN_IMAGE + " ovpn_initpki nopass";
        doSSHCommand(ssh, initPKICommand);
    }

    private String getInitOpenVPNCommand(InstanceParameters config) {
        final StringBuilder command = new StringBuilder("docker run -v ").append(OPENVPN_VPS_FOLDER).append(":/etc/openvpn --rm ").append(OPENVPN_IMAGE).append(" ovpn_genconfig");
        if (config.vpn().dnsServers()!=null && config.vpn().dnsServers().length>0) {
            command.append(" -p 'block-outside-dns'");
            for (String dns : config.vpn().dnsServers()) {
                command.append(" -p 'dhcp-option DNS ").append(dns).append("'");
            }
        }
        command.append(" -u ").append(config.vpn().protocol()).append("://").append(config.vpn().hostname()).append(":").append(config.vpn().port());
        return command.toString();
    }

    /** Starts the remote openvpn server
     * @throws IOException if an error occurs
    */
    public void start(InstanceParameters config) throws IOException {
        // First stop the server if it is running
        final String stopServerCommand = "docker rm -f openvpn || true";
        doSSHCommand(ssh, stopServerCommand);
        final String launchServerCommandFormat = "docker run -d --name openvpn --restart unless-stopped -v %s:/etc/openvpn -p %s:%s --cap-add=NET_ADMIN %s";
        final String launchServerCommand = String.format(launchServerCommandFormat, OPENVPN_VPS_FOLDER, config.vpn().port(), "1194/"+config.vpn().protocol(), OPENVPN_IMAGE);
        logger.debug("Starting openvpn server with command {}", launchServerCommand);
        doSSHCommand(ssh, launchServerCommand);
        logger.debug("Openvpn server is started");
    }

    public record User(String name, boolean valid, Instant expirationDate) {
    }

    /** Lists the remote openvpn server valid users
     * @throws IOException if an error occurs
    */
    public List<User> getUsers() throws IOException {
        try (ListOutputStream outputStream = new ListOutputStream(); ListOutputStream errorStream = new ListOutputStream()) {
            int code = ssh.exec("docker run -v " + OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm kylemanna/openvpn ovpn_listclients", outputStream, errorStream);
            if (code != 0) {
                throw new IOException("Failed to list users with exit code " + code);
            }
            List<String> lines = outputStream.getLines();
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .appendPattern("MMM d HH:mm:ss yyyy z")
                .toFormatter(Locale.ENGLISH);
            return lines.stream().skip(1).map(line -> {
                final String[] parts = line.split(",");
                final String dateString = parts[2].replaceAll("\\s+", " ").trim();
                return new User(parts[0], parts[3].equals("VALID"), ZonedDateTime.parse(dateString, formatter).toInstant());
            }).toList();
        }
    }

    /** Adds a new user to the remote openvpn server
     * @throws IOException if an error occurs
     * @throws UserAlreadyExistsException if the user already exists
    */
    public void addUser(String name) throws IOException {
        final List<User> users = getUsers();
        if (users.stream().anyMatch(user -> user.name().equals(name) && user.valid())) {
            throw new UserAlreadyExistsException(name);
        }
        doSSHCommand(ssh, "docker run -v " + OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OPENVPN_IMAGE + " easyrsa build-client-full " + name + " nopass");
    }

    /** Deletes a user from the remote openvpn server
     * @param name the name of the user
     * @throws IOException if an error occurs
     * @throws UserAlreadyExistsException if the user does not exists
    */
    public void deleteUser(String name) throws IOException {
        final List<User> users = getUsers();
        final Optional<User> user = users.stream().filter(u -> u.name().equals(name)).findAny();
        if (user.isEmpty()) {
            throw new UnknownUserException(name);
        }
        doSSHCommand(ssh, "echo yes | docker run -v " + OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OPENVPN_IMAGE + " ovpn_revokeclient " + name);
    }

    /** Gets the remote openvpn server configuration file for the given user
     * @throws IOException if an error occurs
     * @throws UnknownUserException if the user does not exist
    */
    public List<String> getUserConfigurationFile(String name) throws IOException {
        final List<User> users = getUsers();
        if (users.stream().noneMatch(user -> user.name().equals(name) && user.valid())) {
            throw new UnknownUserException(name);
        }
        try (ListOutputStream outputStream = new ListOutputStream(); ListOutputStream errorStream = new ListOutputStream()) {
            int code = ssh.exec("docker run -v " + OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm kylemanna/openvpn ovpn_getclient " + name, outputStream, errorStream);
            if (code != 0) {
                throw new IOException("Failed to get user configuration file for " + name+" with exit code " + code);
            }
            return outputStream.getLines();
        }
    }

    @Override
    public void close() {
        ssh.close();
    }

    protected void doSSHCommand(Ssh ssh, String command) throws IOException {
        logger.debug("Executing command: {}", command);
//        int code = ssh.exec(command, new LoggerOutputStream(logger, LogLevel.DEBUG), new LoggerOutputStream(logger, LogLevel.DEBUG));
        int code = ssh.exec(command, OutputStream.nullOutputStream(), OutputStream.nullOutputStream());
        logger.debug("Command finished with exit code: {}", code);
        if (code != 0) {
            throw new IOException("Command " + command + " failed with exit code " + code);
        }
    }
}
