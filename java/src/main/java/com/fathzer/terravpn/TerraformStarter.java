package com.fathzer.terravpn;

import static com.fathzer.terravpn.Constants.*;

import java.io.IOException;
import java.io.OutputStream;
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
        // Create the VPS
        createVPS();
        // Update the DNS
        final String ip = getIp();
        logger.info("Updating DDNS for {} with IP {}", config.hostName(), ip);
        config.ddns().provider().updateDns(config.ddns().config(), config.hostName(), ip);

        final String keyPath = sshPrivateKey.toAbsolutePath().toString();
        final String sshUser = VPSProvider.getSSHUser(config.vps());
        try (Ssh ssh = new Ssh(ip, sshUser, keyPath, null)) {
            List<String> extraInitalizationCommands = config.vps().provider().getExtraInitalizationCommands();
            for (String command : extraInitalizationCommands) {
                logger.info("Executing extra initialization command: {}", command);
                doSSHCommand(ssh, command);
            }
        }
        try (OpenVPNManager openVPNConfigManager = new OpenVPNManager(root, ip, sshUser, sshPrivateKey)) {
            // Do openvpn configuration or restore it
            if (openVPNConfigManager.hasBackup()) {
                logger.info("Restoring openvpn configuration");
                openVPNConfigManager.restore();
            } else {
                openVPNConfigManager.initRemote(config);
                logger.info("Saving openvpn configuration");
                openVPNConfigManager.save();
            }
            // Start the server
            openVPNConfigManager.start(config);
        }
    }

    private void createVPS() throws IOException, InterruptedException {
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
    }

    private String getIp() throws IOException {
        // Read the file and remove any non-digit and non-dot characters (to remove at least the trailing newline)
        return Files.readString(root.resolve("ip.txt")).replaceAll("[^0-9.]+", "");
    }

    static void doSSHCommand(Ssh ssh, String command) throws IOException {
        logger.debug("Executing command: {}", command);
//        int code = ssh.exec(command, new LoggerOutputStream(logger, LogLevel.DEBUG), new LoggerOutputStream(logger, LogLevel.WARN));
        int code = ssh.exec(command, OutputStream.nullOutputStream(), OutputStream.nullOutputStream());
        logger.debug("Command finished with exit code: {}", code);
        if (code != 0) {
            throw new IOException("Command " + command + " failed with exit code " + code);
        }
    }
}
