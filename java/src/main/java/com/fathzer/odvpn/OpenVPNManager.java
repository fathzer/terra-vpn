package com.fathzer.odvpn;

import static com.fathzer.odvpn.Constants.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.ssh.Ssh;
import com.fathzer.odvpn.utils.ListOutputStream;

class OpenVPNManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OpenVPNManager.class);
    static final String OPENVPN_TAR_GZ = "openvpn.tar.gz";

    private final Path localFile;
    private final Ssh ssh;

    OpenVPNManager(String address, String sshUser, Path localFile, Path sshPrivateKey) throws IOException {
        this.localFile = localFile.toAbsolutePath();
        this.ssh = new Ssh.Builder(address, sshPrivateKey.toAbsolutePath().toString()).user(sshUser).build();
    }

    /** Checks if the local backup file exists
     * @return true if the local backup file exists
    */
    boolean hasBackup() {
        if (Boolean.getBoolean("forceVPNInit")) {
            logger.info("Force init, skipping backup check");
            return false;
        } else {
            return Files.exists(localFile);
        }
    }

    /** Saves the remote openvpn backup config to the local file
     * @throws IOException if an error occurs
    */
    void save() throws IOException {
        doSSHCommand(ssh, "sudo tar -czf " + OPENVPN_TAR_GZ + " -C "+OPENVPN_VPS_FOLDER+" .");
        ssh.download(OPENVPN_TAR_GZ, localFile.toString());
    }

    /** Uploads and apply the local openvpn backup config to the remote server
     * <br>Note: does not start the server
     * @throws IOException if an error occurs
    */
    void restore() throws IOException {
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
    void initRemote(InstanceParameters config) throws IOException {
        // Erase any previous configuration (if any)
        doSSHCommand(ssh, "sudo rm -rf " + OPENVPN_VPS_FOLDER);
        String initOpenVPNCommand = getInitOpenVPNCommand(config);
        doSSHCommand(ssh, initOpenVPNCommand);
        final String initPKICommand = "echo 'yes' | docker run -v " + OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OPENVPN_IMAGE + " ovpn_initpki nopass";
        doSSHCommand(ssh, initPKICommand);
    }

    private String getInitOpenVPNCommand(InstanceParameters config) {
        final StringBuilder command = new StringBuilder("docker run -v ").append(OPENVPN_VPS_FOLDER).append(":/etc/openvpn --rm ").append(OPENVPN_IMAGE).append(" ovpn_genconfig");
        if (config.dnsServers()!=null && config.dnsServers().length>0) {
            command.append(" -p 'block-outside-dns'");
            for (String dns : config.dnsServers()) {
                command.append(" -p 'dhcp-option DNS ").append(dns).append("'");
            }
        }
        command.append(" -u ").append(config.protocol()).append("://").append(config.hostName()).append(":").append(config.port());
        return command.toString();
    }

    void start(InstanceParameters config) throws IOException {
        // First stop the server if it is running
        final String stopServerCommand = "docker rm -f openvpn || true";
        doSSHCommand(ssh, stopServerCommand);
        final String launchServerCommandFormat = "docker run -d --name openvpn --restart unless-stopped -v %s:/etc/openvpn -p %s:%s --cap-add=NET_ADMIN %s";
        final String launchServerCommand = String.format(launchServerCommandFormat, OPENVPN_VPS_FOLDER, config.port(), "1194/"+config.protocol(), OPENVPN_IMAGE);
        logger.debug("Starting openvpn server with command {}", launchServerCommand);
        doSSHCommand(ssh, launchServerCommand);
        logger.debug("Openvpn server is started");
    }

    void addUser(String name) throws IOException {
        doSSHCommand(ssh, "docker run -v " + OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OPENVPN_IMAGE + " easyrsa build-client-full " + name + " nopass");
    }

    List<String> getUserConfigurationFile(String name) throws IOException {
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

    private static void doSSHCommand(Ssh ssh, String command) throws IOException {
        logger.debug("Executing command: {}", command);
//        int code = ssh.exec(command, new LoggerOutputStream(logger, LogLevel.DEBUG), new LoggerOutputStream(logger, LogLevel.DEBUG));
        int code = ssh.exec(command, OutputStream.nullOutputStream(), OutputStream.nullOutputStream());
        logger.debug("Command finished with exit code: {}", code);
        if (code != 0) {
            throw new IOException("Command " + command + " failed with exit code " + code);
        }
    }

}
