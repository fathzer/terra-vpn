package com.fathzer.odvpn;

import static com.fathzer.odvpn.CommandParser.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.utils.IOLambdas;

class CommandParserTest {
    private static final CommandParser PARSER = new CommandParser();

    @BeforeAll
    static void setUp() {
        PARSER.setSilent(true);
    }

    @Test
    void testInitCommand() throws IOException {
        final ODVpn odvpn = mock(ODVpn.class);
        
        // Test init command without force
        String[] args = {Command.INIT.name, "my-vpn", "config.json"};
        IOLambdas.IORunnable command = PARSER.parse(args, odvpn);
        assertNotNull(command);
        command.run();
        verify(odvpn).init("my-vpn", Paths.get("config.json"), null, false);
        
        // Test init command with force
        String[] argsWithForce = {Command.INIT.name, "-f", "my-vpn", "config.json"};
        command = PARSER.parse(argsWithForce, odvpn);
        assertNotNull(command);
        command.run();
        verify(odvpn).init("my-vpn", Paths.get("config.json"), null, true);

        // Test init command with openvpn config file and force
        String[] argsWithOpenVpnConfig = {Command.INIT.name, "-f", "my-vpn", "config.json", "configovpn.tar.gz"};
        command = PARSER.parse(argsWithOpenVpnConfig, odvpn);
        assertNotNull(command);
        command.run();
        verify(odvpn).init("my-vpn", Paths.get("config.json"), Paths.get("configovpn.tar.gz"), true);
    }

    @Test
    void testStartCommand() throws IOException {
        final ODVpn odvpn = mock(ODVpn.class);
        String[] args = {Command.START.name, "my-vpn"};
        IOLambdas.IORunnable command = PARSER.parse(args, odvpn);
        assertNotNull(command);
        command.run();
        verify(odvpn).start("my-vpn");
    }

    @Test
    void testDeleteCommand() throws IOException {
        final ODVpn odvpn = mock(ODVpn.class);
        String[] args = {Command.DELETE.name, "my-vpn"};
        IOLambdas.IORunnable command = PARSER.parse(args, odvpn);
        assertNotNull(command);
        command.run();
        verify(odvpn).delete("my-vpn", false);
    }

    @Test
    void testInvalidCommand() {
        final ODVpn odvpn = mock(ODVpn.class);
        String[] invalidArgs = {"invalid", "my-vpn"};
        assertNull(PARSER.parse(invalidArgs, odvpn));
        String[] missingCommandArgs = {"my-vpn"};
        assertNull(PARSER.parse(missingCommandArgs, odvpn));
        String[] tooManyArgs = {Command.INIT.name, "-f", "my-vpn", "config.json", "configovpn.tar.gz", "extra"};
        assertNull(PARSER.parse(tooManyArgs, odvpn));
        String[] tooManyArgs2 = {Command.INIT.name, "my-vpn", "config.json", "configovpn.tar.gz", "extra"};
        assertNull(PARSER.parse(tooManyArgs2, odvpn));
        String[] invertedArgs = {Command.INIT.name, "my-vpn", "-f", "config.json"};
        assertNull(PARSER.parse(invertedArgs, odvpn));
    }

    @Test
    void testMissingName() {
        final ODVpn odvpn = mock(ODVpn.class);
        String[] initArgs = {Command.INIT.name, "config.json"};
        assertNull(PARSER.parse(initArgs, odvpn));
        String[] deleteArgs = {Command.DELETE.name};
        assertNull(PARSER.parse(deleteArgs, odvpn));
    }

    @Test
    void testExtraOption() {
        final ODVpn odvpn = mock(ODVpn.class);
        String[] initArgs = {Command.INIT.name, "my-vpn", "-x", "extra.json"};
        assertNull(PARSER.parse(initArgs, odvpn));
        String[] startArgs = {Command.START.name, "my-vpn", "-x"};
        assertNull(PARSER.parse(startArgs, odvpn));
    }
}
