package com.fathzer.terravpn;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

class CommandParser {
    private static final String COMMAND_PREFIX = "terravpn";
    private static final String OPT_FORCE = "f";
    private static final String OPT_FORCE_LONG = "force";

    static final String INIT_COMMAND = "init";
    static final String START_COMMAND = "start";
    static final String STOP_COMMAND = "stop";
    static final String DELETE_COMMAND = "delete";

    private boolean silent;

    Command parse(String[] args) {
        final PreParsedCommand preParsedCommand = checkCommand(args);
        if (preParsedCommand != null) {
            try {
                final CommandLine line = new DefaultParser().parse(preParsedCommand.getOptions(), preParsedCommand.args());
                checkArgsOrder(line, preParsedCommand.args());
                final String name;
                if (line.getArgList().isEmpty()) {
                    throw new IllegalArgumentException("Name is required");
                } else {
                    name = line.getArgList().remove(0);
                }
                final String command = preParsedCommand.command();
                if (command.equals(INIT_COMMAND)) {
                    if (line.getArgList().isEmpty()) {
                        throw new IllegalArgumentException("Configuration file is required");
                    }
                    if (line.getArgList().size() > 1) {
                        throw new IllegalArgumentException("Too many arguments");
                    }
                    return new Command(command, name, Path.of(line.getArgList().get(0)), line.hasOption(OPT_FORCE));
                } else {
                    return new Command(command, name, null, false);
                }
            } catch (IllegalArgumentException | ParseException e) {
                LoggerFactory.getLogger(CommandParser.class).debug("Failed to parse command line arguments", e);
                if (!silent) {
                    final HelpFormatter formatter = new HelpFormatter();
                    String prefix = COMMAND_PREFIX + " " + preParsedCommand.command() + " [options]" + " name";
                    if (preParsedCommand.command().equals(INIT_COMMAND)) {
                        prefix += " configFile";
                    }
                    formatter.printHelp(prefix, preParsedCommand.getOptions());
                }
            }
        }
        return null;
    }

    /**
     * Checks that the arguments are at the end of the command.
     * <br>If you prefer, it checks that options are before arguments.
     * @param line the parsed command line
     * @param args the command arguments (including options)
     */
    private void checkArgsOrder(CommandLine line, String[] args) {
        final List<String> commandArgs = line.getArgList();
        final int argCount = commandArgs.size();
        final List<String> argsList = new LinkedList<>(Arrays.asList(args)).subList(args.length-argCount, args.length);
        if (!commandArgs.equals(argsList)) {
            throw new IllegalArgumentException("Arguments must be at the end of the command");
        }
    }

    private static record PreParsedCommand(String command, String[] args) {
        private Options getOptions() {
            final Options options = new Options();
            switch (command) {
                case INIT_COMMAND:
                    options.addOption(OPT_FORCE, OPT_FORCE_LONG, false, "Force initialization");
                    break;
                case DELETE_COMMAND:
                    options.addOption(OPT_FORCE, OPT_FORCE_LONG, false, "Force deletion");
                    break;
                case START_COMMAND, STOP_COMMAND:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown command: " + command);
            }
            return options;
        }
    }

    private PreParsedCommand checkCommand(String[] args) {
        if (args.length > 0) {
            final String command = args[0];
            if (command.equals(INIT_COMMAND) || command.equals(START_COMMAND) || command.equals(STOP_COMMAND) || command.equals(DELETE_COMMAND)) {
                return new PreParsedCommand(command, args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
            }
        }
        if (!silent) {
            StringBuilder sb = new StringBuilder();
            sb.append(INIT_COMMAND).append(" Initialize a new VPN server configuration").append('\n');
            sb.append(START_COMMAND).append(" Start the VPN server").append('\n');
            sb.append(STOP_COMMAND).append(" Stop the VPN server").append('\n');
            sb.append(DELETE_COMMAND).append(" Delete the VPN server configuration");
            new HelpFormatter().printHelp(COMMAND_PREFIX+" command name", "", new Options(), "Available commands are:\n" + sb.toString());
        }
        return null;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }
}
