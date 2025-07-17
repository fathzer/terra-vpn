package com.fathzer.odvpn;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.utils.IOLambdas;
import com.fathzer.odvpn.utils.IOLambdas.IOBiConsumer;

class CommandParser {
    private static final String COMMAND_PREFIX = "java -jar odvpn.jar";
    private static final String OPT_FORCE = "f";
    private static final String OPT_FORCE_LONG = "force";

    enum Command {
        WEB("web", "Launch the web application", "", new Options(), (o, l) -> o.web()),
        INIT("init", "Initialize a new VPN server configuration", "name configFile [openVPNConfigFile]", getInitOptions(), (o, l) -> o.init(l.getArgList().get(0), Path.of(l.getArgList().get(1)), l.getArgList().size() > 2 ? Path.of(l.getArgList().get(2)) : null, l.hasOption(OPT_FORCE))),
        START("start", "Start the VPN server", "name", new Options(), (o, l) -> o.start(l.getArgList().get(0))),
        STOP("stop", "Stop the VPN server", "name", new Options(), (o, l) -> o.stop(l.getArgList().get(0))),
        DELETE("delete", "Delete the VPN server configuration", "name", getDeleteOptions(), (o, l) -> o.delete(l.getArgList().get(0), l.hasOption(OPT_FORCE)));

        private static final Map<String, Command> COMMANDS = new HashMap<>();
        static {
            for (Command command : values()) {
                COMMANDS.put(command.name, command);
            }
        }
        final String name;
        private final String description;
        private final String argsDescription;
        private final Options options;
        private final IOBiConsumer<ODVpn, CommandLine> action;

        private Command(String name, String description, String argsDescription, Options options, IOBiConsumer<ODVpn, CommandLine> action) {
            this.name = name;
            this.description = description;
            this.argsDescription = argsDescription;
            this.options = options;
            this.action = action;
        }

        private void checkArguments(CommandLine line) {
            final int min;
            final int max;
            if (argsDescription.isEmpty()) {
                min = 0;
                max = 0;
            } else {
                final String[] args = argsDescription.split(" ");
                min = (int) Arrays.stream(args).filter(a -> !a.startsWith("[")).count();
                max = args.length;
            }
            final int count = line.getArgList().size();
            if (count < min || count > max) {
                throw new IllegalArgumentException("Invalid number of arguments");
            }
        }

        static Command fromName(String name) {
            return COMMANDS.get(name);
        }

        private static Options getInitOptions() {
            final Options options = new Options();
            options.addOption(OPT_FORCE, OPT_FORCE_LONG, false, "Force initialization");
            return options;
        }

        private static Options getDeleteOptions() {
            final Options options = new Options();
            options.addOption(OPT_FORCE, OPT_FORCE_LONG, false, "Force deletion");
            return options;
        }
   }

    private boolean silent;

    IOLambdas.IORunnable parse(String[] args, ODVpn odvpn) {
        final PreParsedCommand preParsedCommand = checkCommand(args);
        if (preParsedCommand != null) {
            try {
                final CommandLine line = new DefaultParser().parse(preParsedCommand.command().options, preParsedCommand.args());
                checkArgsOrder(line, preParsedCommand.args());
                final Command command = preParsedCommand.command();
                command.checkArguments(line);
                return () -> command.action.accept(odvpn, line);
            } catch (IllegalArgumentException | ParseException e) {
                LoggerFactory.getLogger(CommandParser.class).debug("Failed to parse command line arguments", e);
                if (!silent) {
                    final HelpFormatter formatter = new HelpFormatter();
                    String prefix = COMMAND_PREFIX + " " + preParsedCommand.command().name;
                    if (!preParsedCommand.command().options.getOptions().isEmpty()) {
                        prefix += " [options]";
                    }
                    String argsDescription = preParsedCommand.command().argsDescription;
                    if (!argsDescription.isEmpty()) {
                        prefix += " " + argsDescription;
                    }
                    formatter.printHelp(prefix, preParsedCommand.command().options);
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

    private static record PreParsedCommand(Command command, String[] args) {
    }

    private PreParsedCommand checkCommand(String[] args) {
        if (args.length > 0) {
            final Command command = Command.fromName(args[0]);
            if (command != null) {
                return new PreParsedCommand(command, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        if (!silent) {
            StringBuilder sb = new StringBuilder();
            for (Command command : Command.values()) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(command.name).append(" ").append(command.description);
            }
            new HelpFormatter().printHelp(COMMAND_PREFIX+" command name", "", new Options(), "Available commands are:\n" + sb.toString());
        }
        return null;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }
}
