package com.fathzer.terravpn;

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
        }

        final Path configPath = command.configPath();
        logger.info("Configuration file: {}", configPath.toAbsolutePath());
        final Configuration config = Configuration.fromJson(configPath);
        
        if (logger.isInfoEnabled()) {
            logger.info("Configuration: {}", config);
            logger.info("VPS provider: {}", config.vpsProvider().name());
            logger.info(config.ddnsProvider().name());
        }

        final TerraformBuilder builder = new TerraformBuilder(config, DATA_DIR.resolve(command.name()));
        System.out.println("----------------- variables.tf -----------------");
        builder.buildVariables(System.out::println);
        System.out.println("----------------- terraform.tfvars -----------------");
        builder.buildVariablesValues(System.out::println);
        System.out.println("----------------- main.tf -----------------");
        builder.buildMainScript(System.out::println);

        builder.build();
    }

}
