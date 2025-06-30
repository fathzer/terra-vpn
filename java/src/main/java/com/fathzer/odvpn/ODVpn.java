package com.fathzer.odvpn;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.CommandParser.IORunnable;
import com.fathzer.odvpn.json.InstanceParametersParser;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.ws.ODVpnApplication;

public class ODVpn {
    /** System property to set the data directory */
    public static final String DATA_DIR_PROPERTY = "data.dir";
    /** Default data directory */
    public static final String DEFAULT_DATA_DIR = "data";

    private static final Logger logger = LoggerFactory.getLogger(ODVpn.class);

    static final Path DATA_DIR = Path.of(System.getProperty(DATA_DIR_PROPERTY, DEFAULT_DATA_DIR));

    public static void main(String[] args) throws IOException {
        final IORunnable command = new CommandParser().parse(args, new ODVpn());
        if (command == null) {
            System.exit(1);
        } else {
            command.run();
        }
    }

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

        final Path directory = DATA_DIR.resolve(name);
        final OnDemandVPNManager manager = new OnDemandVPNManager(config, directory, Paths.get("ssh/id_rsa")); //TODO: make it configurable
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }
}
