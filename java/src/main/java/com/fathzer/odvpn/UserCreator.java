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
    private final InstanceParameters parameters;
    private final Path sshPrivateKey;
    private final Path root;

    public UserCreator(InstanceParameters parameters, Path sshPrivateKey, Path root) {
        this.parameters = parameters;
        this.sshPrivateKey = sshPrivateKey;
        this.root = root;
    }

    public static void main(String[] args) throws IOException {
        final InstanceParameters parameters = InstanceParametersParser.parse(Path.of("data/myvpn/config.json"));
        new UserCreator(parameters, Path.of("ssh/id_rsa"), Path.of("data/myvpn")).createUser(args[0]);
    }

    public void createUser(String username) throws IOException {
        final String ip = getIp();
        final String sshUser = VPSProvider.getSSHUser(parameters.vps());
        try (OpenVPNManager openVPNConfigManager = new OpenVPNManager(ip, sshUser, sshPrivateKey)) {
            openVPNConfigManager.addUser(username);
            List<String> configFile = openVPNConfigManager.getUserConfigurationFile(username);
            Files.write(root.resolve(username + ".ovpn"), configFile);
            logger.info("User {} added", username);
            System.out.println(String.join("\n", configFile));
            logger.info("Saving openvpn configuration");
            openVPNConfigManager.save(root.resolve("openvpn.tar.gz"));
        }
    }

    private String getIp() throws IOException {
        // Read the file and remove any non-digit and non-dot characters (to remove at least the trailing newline)
        return Files.readString(root.resolve("ip.txt")).replaceAll("[^0-9.]+", "");
    }
}

