package com.fathzer.terravpn;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerraVPN {
    private static final Logger logger = LoggerFactory.getLogger(TerraVPN.class);

    private static final String OPT_CONFIG = "c";
    private static final String OPT_CONFIG_LONG = "config";
    private static final String OPT_TARGET = "t";
    private static final String OPT_TARGET_LONG = "target";
    
    public static void main(String[] args) throws IOException {
        final CommandLine line = parseArgs(args);

        final Path configPath = Paths.get(line.getOptionValue(OPT_CONFIG));
        logger.info("Configuration file: {}", configPath.toAbsolutePath());
        final Configuration config = Configuration.fromJson(configPath);
        
        if (logger.isInfoEnabled()) {
            logger.info("Configuration: {}", config);
            logger.info("VPS provider: {}", config.vpsProvider().name());
            logger.info(config.ddnsProvider().name());
            logger.info("Arguments: {}", line.getArgList());
        }

        final TerraformBuilder builder = new TerraformBuilder(config, Paths.get(line.getOptionValue(OPT_TARGET)+"/"+config.name())); //TODO
        System.out.println("----------------- variables.tf -----------------");
        builder.buildVariables(System.out::println);
        System.out.println("----------------- terraform.tfvars -----------------");
        builder.buildVariablesValues(System.out::println);
        System.out.println("----------------- main.tf -----------------");
        builder.buildMainScript(System.out::println);

        builder.build();
    }

    static CommandLine parseArgs(String[] args) {
        final Options options = new Options();
        final CommandLineParser parser = new DefaultParser();
        options.addRequiredOption(OPT_CONFIG, OPT_CONFIG_LONG, true, "Configuration file");
        options.addRequiredOption(OPT_TARGET, OPT_TARGET_LONG, true, "Terraform directory");
        try {
            return parser.parse(options, args);
        } catch (Exception e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("terravpn", options);
            throw new IllegalArgumentException("Failed to parse command line arguments", e);
        }
    }
}
