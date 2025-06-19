package com.fathzer.terravpn;

import static com.fathzer.terravpn.CommandParser.*;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerraVPN {
    /** System property to set the data directory */
    public static final String DATA_DIR_PROPERTY = "data.dir";
    /** Default data directory */
    public static final String DEFAULT_DATA_DIR = "data";

    private static final Logger logger = LoggerFactory.getLogger(TerraVPN.class);

    private static final Path DATA_DIR = Path.of(System.getProperty(DATA_DIR_PROPERTY, DEFAULT_DATA_DIR));

    public static void main(String[] args) throws IOException {
        final Command command = new CommandParser().parse(args);
        if (command == null) {
            System.exit(1);
        } else if (INIT_COMMAND.equals(command.command())) {
            new TerraVPN(DATA_DIR.resolve(command.name())).init(command.configPath(), command.force());
        } else if (START_COMMAND.equals(command.command())) {
            new TerraVPN(DATA_DIR.resolve(command.name())).start();
        } else if (STOP_COMMAND.equals(command.command())) {
            new TerraVPN(DATA_DIR.resolve(command.name())).stop();
        } else if (CommandParser.DELETE_COMMAND.equals(command.command())) {
            new TerraVPN(DATA_DIR.resolve(command.name())).delete(command.force());
        }
    }

     private final Path directory;

    private TerraVPN(Path path) {
        this.directory = path;
    }

    private void init(final Path configPath, final boolean force) throws IOException {
        logger.info("Configuration file: {}", configPath.toAbsolutePath());
        final Configuration config = Configuration.fromJson(configPath);
        
        if (logger.isInfoEnabled()) {
            logger.info("VPS provider: {}", config.vpsProvider().name());
            logger.info("DDNS provider: {}", config.ddnsProvider().name());
        }

        final TerraformBuilder builder = new TerraformBuilder(config, directory);
        logger.info("Writing configuration files to directory: {}", directory.toAbsolutePath());

        builder.build();
        logger.info("Finished");
    }

    private void start() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

    private void stop() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'stop'");
    }

    private void delete(boolean force) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }
}
