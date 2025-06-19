package com.fathzer.terravpn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fathzer.terravpn.ssh.Ssh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerraformStarter {
    private static final Logger logger = LoggerFactory.getLogger(TerraformStarter.class);
    private final Path root;

    public TerraformStarter(Path root) {
        this.root = root;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new TerraformStarter(Path.of("data/myvpn")).start();
    }

    public void start() throws IOException, InterruptedException {
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

        final String ip = getIp();
        System.out.println("VPN IP: " + ip+".");
        logger.info("VPN IP: {}", ip);
        Configuration config = Configuration.fromJson(root.resolve("config.json"));
        config.ddnsProvider().updateDns(config, ip);
        logger.info("DDNS updated");
        try (Ssh ssh = new Ssh(ip, config.sshKeysFolder().resolve("id_rsa").toAbsolutePath().toString())) {
            ssh.exec("docker pull kylemanna/openvpn", System.out, System.err);
        }

    }

    private String getIp() throws IOException {
        // Read the file and remove any non-digit and non-dot characters (to remove at least the trailing newline)
        return Files.readString(root.resolve("ip.txt")).replaceAll("[^0-9.]+", "");
    }
}
