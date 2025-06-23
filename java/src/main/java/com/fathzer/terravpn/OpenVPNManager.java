package com.fathzer.terravpn;

import static com.fathzer.terravpn.Constants.*;
import static com.fathzer.terravpn.TerraformStarter.doSSHCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.terravpn.repository.InstanceParameters;
import com.fathzer.terravpn.ssh.Ssh;
import com.fathzer.terravpn.utils.ListOutputStream;

class OpenVPNManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OpenVPNManager.class);
    private static final String OPENVPN_TAR_GZ = "openvpn.tar.gz";

    private final Path localFile;
    private final Ssh ssh;

    OpenVPNManager(Path root, String address, String sshUser, Path sshPrivateKey) throws IOException {
        this.localFile = root.resolve(OPENVPN_TAR_GZ).toAbsolutePath();
        this.ssh = new Ssh(address, sshUser, sshPrivateKey.toAbsolutePath().toString(), null);
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
        doSSHCommand(ssh, "sudo tar -czf " + OPENVPN_TAR_GZ + " --transform='s|^"+OPENVPN_VPS_FOLDER+"|openvpn|' "+OPENVPN_VPS_FOLDER);
        ssh.download(OPENVPN_TAR_GZ, localFile.toString());
    }

    /** Uploads and apply the local openvpn backup config to the remote server
     * <br>Note: does not start the server
     * @throws IOException if an error occurs
    */
    void restore() throws IOException {
        ssh.upload(localFile.toString(), OPENVPN_TAR_GZ);
        // TODO remove the ls -l
        final List<String> commands = Arrays.asList(
			"#!/bin/bash",
			"set -e",
			"sudo tar -xzf " + OPENVPN_TAR_GZ,
			"rm "+OPENVPN_TAR_GZ,
			"ls -l",
			"sudo rm -rf "+OPENVPN_VPS_FOLDER,
			"sudo mv openvpn "+OPENVPN_VPS_FOLDER,
			"ls -l "+OPENVPN_VPS_FOLDER
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
        logger.info("Initializing openvpn configuration with command{}", initOpenVPNCommand);
        doSSHCommand(ssh, initOpenVPNCommand);
        final String initPKICommand = "echo 'yes' | docker run -v " + OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OPENVPN_IMAGE + " ovpn_initpki nopass";
        logger.info("Set the Public Key Infrastructure (can be long) with command {}", initPKICommand);
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
        final String launchServerCommand = String.format(launchServerCommandFormat, OPENVPN_VPS_FOLDER, config.port(), config.port()+"/"+config.protocol(), OPENVPN_IMAGE);
        logger.info("Starting openvpn server with command {}", launchServerCommand);
        doSSHCommand(ssh, launchServerCommand);
        logger.info("Openvpn server ready");
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
}
