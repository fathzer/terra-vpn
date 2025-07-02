package com.fathzer.odvpn;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.CommandParser.IORunnable;
import com.fathzer.odvpn.json.InstanceParametersParser;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.VPNRepositorySettings;
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
            logger.info("VPS provider: {}", config.vps().provider().name());
            logger.info("DDNS provider: {}", config.ddns().provider().name());
        }

        final Path directory = settings.dataPath().resolve(name);
        final OnDemandVPNManager manager = new OnDemandVPNManager(config, directory, settings.privateKeyPath());
        logger.info("Writing configuration files to directory: {}", directory.toAbsolutePath());

        manager.init(openVpnConfigPath, force);
        logger.info("Finished");
    }

    void start(final String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

    void stop(final String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stop'");
    }

    void delete(final String name, final boolean force) {
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }
}
