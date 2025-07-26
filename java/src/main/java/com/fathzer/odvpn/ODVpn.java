package com.fathzer.odvpn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.json.InstanceParametersParser;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.VPNRepositorySettings;
import com.fathzer.odvpn.utils.IOLambdas.IORunnable;
import com.fathzer.odvpn.ws.ODVpnApplication;

public class ODVpn {
    private static final Logger logger = LoggerFactory.getLogger(ODVpn.class);

    public static void main(String[] args) throws IOException {
        final ODVpn odvpn = new ODVpn();
		final IORunnable command = new CommandParser().parse(args, odvpn);
        if (command == null) {
            System.exit(1);
        } else {
        	odvpn.settings = VPNRepositorySettings.fromEnvironment(false);
            command.run();
        }
    }

    private VPNRepositorySettings settings;

    void web() {
        ODVpnApplication.main(new String[0]);
    }

    void init(final String name, final Path configPath, final Path openVpnConfigPath, final boolean force) throws IOException {
        logger.info("Configuration file: {}", configPath.toAbsolutePath());
        final InstanceParameters config = InstanceParametersParser.parse(configPath);
        
        if (logger.isInfoEnabled()) {
            logger.info("VPS provider: {}", config.vps().name());
            logger.info("DDNS provider: {}", config.ddns().name());
        }

        final Path directory = settings.dataPath().resolve(name);
        final AbstractOnDemandVPNManager manager = getManager(name);
        logger.info("Writing configuration files to directory: {}", directory.toAbsolutePath());

        try (InputStream openVpnConfigStream = openVpnConfigPath != null ? Files.newInputStream(openVpnConfigPath) : null) {
            manager.init(openVpnConfigStream, force);
        }
        logger.info("Finished");
    }

    private AbstractOnDemandVPNManager getManager(final String name) throws IOException {
        return new LocalDiskOnDemandVPNManager(settings, name);
    }

    void start(final String name) throws IOException {
        getManager(name).start(new MyStartProgressListener());
    }

    void stop(final String name) throws IOException {
        getManager(name).stop();
    }

    void delete(final String name, final boolean force) throws IOException {
        getManager(name).delete(force);
    }

    private class MyStartProgressListener implements StartProgressListener {
        @Override
        public void creatingVPS(VPSProvider.VPSState state) {
            logger.info("VPS state: {}", state);
        }

        @Override
        public void updatingDDNS(String hostName, String ip) {
            logger.info("Updating DDNS for {} with IP {}", hostName, ip);
        }

        @Override
        public void waitingSSHConnection(String ip) {
            logger.info("Waiting for SSH connection to {}", ip);
        }

        @Override
        public void restoringOpenVPNConfiguration() {
            logger.info("Restoring openvpn configuration");
        }

        @Override
        public void creatingOpenVPNConfiguration() {
            logger.info("Creating openvpn configuration (can be long)");
        }

        @Override
        public void startingOpenVPNServer() {
            logger.info("Starting openvpn server");
        }

        @Override
        public void waitingDNSPropagation() {
            logger.info("Waiting for DNS propagation");
        }

        @Override
        public void ready() {
            logger.info("Openvpn server is ready");
        }
    }
}
