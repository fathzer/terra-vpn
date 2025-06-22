package com.fathzer.terravpn;

import static com.fathzer.terravpn.Constants.*;
import static com.fathzer.terravpn.TerraformStarter.doSSHCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.fathzer.terravpn.ssh.Ssh;

class OpenVPNConfigManager {
    private static final String OPENVPN_TAR_GZ = "openvpn.tar.gz";

    private final Path localFile;
    private final String address;
    private final String sshUser;
    private final String sshPrivateKey;

    OpenVPNConfigManager(Path root, String address, String sshUser, Path sshPrivateKey) {
        this.localFile = root.resolve(OPENVPN_TAR_GZ).toAbsolutePath();
        this.address = address;
        this.sshUser = sshUser;
        this.sshPrivateKey = sshPrivateKey.toAbsolutePath().toString();
    }

    /** Retrieves the remote openvpn config and downloads it to the local file
     * @throws IOException if an error occurs
    */
    void get() throws IOException {
        try (Ssh ssh = new Ssh(address, sshUser, sshPrivateKey, null)) {
            doSSHCommand(ssh, "tar -czf " + OPENVPN_TAR_GZ + " --transform='s|^"+OPENVPN_VPS_FOLDER+"|openvpn|' "+OPENVPN_VPS_FOLDER);
            ssh.download(OPENVPN_TAR_GZ, localFile.toString());
        }
    }

    /** Uploads and apply the local openvpn config to the remote server
     * @throws IOException if an error occurs
    */
    void set() throws IOException {
        try (Ssh ssh = new Ssh(address, sshUser, sshPrivateKey, null)) {
            ssh.upload(localFile.toString(), OPENVPN_TAR_GZ);
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
    }

    boolean localFileExists() {
        return Files.exists(localFile);
    }

    public static void main(String[] args) throws IOException {
        OpenVPNConfigManager manager = new OpenVPNConfigManager(Path.of("data/myvpn"), "terravpn.soon.it", "root", Path.of("ssh/id_rsa"));
        manager.get();
    }
}
