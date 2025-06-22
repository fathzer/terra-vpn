package com.fathzer.terravpn;

import static com.fathzer.terravpn.Constants.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fathzer.terravpn.json.InstanceParametersParser;
import com.fathzer.terravpn.repository.InstanceParameters;
import com.fathzer.terravpn.ssh.Ssh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerraformStarter {
    private static final Logger logger = LoggerFactory.getLogger(TerraformStarter.class);
    private final Path root;
    private final Path sshPrivateKey;

    public TerraformStarter(Path root, Path sshPrivateKey) {
        this.root = root;
        this.sshPrivateKey = sshPrivateKey;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new TerraformStarter(Path.of("data/myvpn"), Path.of("ssh/id_rsa")).start();
    }

    public void start() throws IOException, InterruptedException {
        // Load the instance parameters
        InstanceParameters config = InstanceParametersParser.parse(root.resolve("config.json"));
        // Do terraform apply
        logger.info("Creating server");
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("terraform", "apply", "-auto-approve");
        builder.directory(root.toFile());
        builder.inheritIO();
        logger.info("Running terraform apply");
        Process process = builder.start();
        process.waitFor();
        final int exitCode = process.exitValue();
        if (exitCode != 0) {
            logger.error("Terraform finished with exit code {}", exitCode);
            System.exit(exitCode);
        }

        // Update the DNS
        final String ip = getIp();
        logger.info("Updating DDNS for {} with IP {}", config.hostName(), ip);
        config.ddns().provider().updateDns(config.ddns().config(), config.hostName(), ip);

        // Do openvpn configuration
        final String keyPath = sshPrivateKey.toAbsolutePath().toString();
        try (Ssh ssh = new Ssh(ip, keyPath)) {
            List<String> extraInitalizationCommands = config.vps().provider().getExtraInitalizationCommands();
            for (String command : extraInitalizationCommands) {
                logger.info("Executing extra initialization command: {}", command);
                doSSHCommand(ssh, command);
            }
            logger.info("Getting openvpn docker image");
            doSSHCommand(ssh, "docker pull " + OPENVPN_IMAGE);
            final OpenVPNConfigManager openVPNConfigManager = new OpenVPNConfigManager(root, ip, "root", sshPrivateKey);
            if (openVPNConfigManager.localFileExists()) {
                logger.info("Restoring openvpn configuration");
                openVPNConfigManager.set();
            } else {
                String initOpenVPNCommand = getInitOpenVPNCommand(config);
                logger.info("Initializing openvpn configuration with command{}", initOpenVPNCommand);
                doSSHCommand(ssh, initOpenVPNCommand);
                final String initPKICommand = "echo 'yes' | docker run -v " + OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OPENVPN_IMAGE + " ovpn_initpki nopass";
                logger.info("Set the Public Key Infrastructure (can be long) with command {}", initPKICommand);
                doSSHCommand(ssh, initPKICommand);
                logger.info("Saving openvpn configuration");
                openVPNConfigManager.get();
            }
            // Launch the server
            // First stop the server if it is running
            final String stopServerCommand = "docker rm -f openvpn || true";
            doSSHCommand(ssh, stopServerCommand);
            final String launchServerCommandFormat = "docker run -d --name openvpn --restart unless-stopped -v %s:/etc/openvpn -p %s:%s --cap-add=NET_ADMIN %s";
            final String launchServerCommand = String.format(launchServerCommandFormat, OPENVPN_VPS_FOLDER, config.port(), config.port()+"/"+config.protocol(), OPENVPN_IMAGE);
            logger.info("Starting openvpn server with command {}", launchServerCommand);
            doSSHCommand(ssh, launchServerCommand);
            logger.info("Openvpn server ready");
        }
    }

    private String getIp() throws IOException {
        // Read the file and remove any non-digit and non-dot characters (to remove at least the trailing newline)
        return Files.readString(root.resolve("ip.txt")).replaceAll("[^0-9.]+", "");
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
        command.append(" -p 'redirect-gateway def1'");
        return command.toString();
    }

    static void doSSHCommand(Ssh ssh, String command) throws IOException {
        logger.debug("Executing command: {}", command);
        int code = ssh.exec(command, System.out, System.err);
        logger.debug("Command finished with exit code: {}", code);
        if (code != 0) {
            throw new IOException("Command " + command + " failed with exit code " + code);
        }
    }
}
