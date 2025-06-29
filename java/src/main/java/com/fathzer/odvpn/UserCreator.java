package com.fathzer.odvpn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.json.InstanceParametersParser;
import com.fathzer.odvpn.repository.InstanceParameters;

public class UserCreator {
    private static final Logger logger = LoggerFactory.getLogger(UserCreator.class);
    private final Path root;
    private final Path sshPrivateKey;

    public UserCreator(Path root, Path sshPrivateKey) {
        this.root = root;
        this.sshPrivateKey = sshPrivateKey;
    }


    public static void main(String[] args) throws IOException {
        new UserCreator(Path.of("data/myvpn"), Path.of("ssh/id_rsa")).createUser("jma");
    }

    public void createUser(String username) throws IOException {
        // Load the instance parameters
        InstanceParameters config = InstanceParametersParser.parse(root.resolve("config.json"));

        final String ip = getIp();
        final String sshUser = VPSProvider.getSSHUser(config.vps());
        try (OpenVPNManager openVPNConfigManager = new OpenVPNManager(root, ip, sshUser, sshPrivateKey)) {
            openVPNConfigManager.addUser(username);
            List<String> configFile = openVPNConfigManager.getUserConfigurationFile(username);
            Files.write(root.resolve(username + ".ovpn"), configFile);
            logger.info("User {} added", username);
            System.out.println(String.join("\n", configFile));
            logger.info("Saving openvpn configuration");
            openVPNConfigManager.save();
        }
    }

    private String getIp() throws IOException {
        // Read the file and remove any non-digit and non-dot characters (to remove at least the trailing newline)
        return Files.readString(root.resolve("ip.txt")).replaceAll("[^0-9.]+", "");
    }
}

