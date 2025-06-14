package com.fathzer.terravpn;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;

import static com.fathzer.terravpn.CommandParser.*;


import org.junit.jupiter.api.Test;

class CommandParserTest {
    private static final CommandParser PARSER = new CommandParser();

    @BeforeAll
    static void setUp() {
        PARSER.setSilent(true);
    }

    @Test
    void testInitCommand() {
        String[] args = {INIT_COMMAND, "my-vpn", "config.json"};
        Command command = PARSER.parse(args);
        assertEquals("my-vpn", command.name());
        assertEquals("config.json", command.configPath().getFileName().toString());
        assertFalse(command.force());
        assertEquals(INIT_COMMAND, command.command());
        String[] argsWithForce = {INIT_COMMAND, "-f", "my-vpn", "config.json"};
        Command forcedCommand = PARSER.parse(argsWithForce);
        assertTrue(forcedCommand.force());
    }

    @Test
    void testStartCommand() {
        String[] args = {START_COMMAND, "my-vpn"};
        Command command = PARSER.parse(args);
        assertEquals(START_COMMAND, command.command());
        assertNull(command.configPath());
    }

    @Test
    void testDeleteCommand() {
        String[] args = {DELETE_COMMAND, "my-vpn"};
        Command command = PARSER.parse(args);
        assertEquals(DELETE_COMMAND, command.command());
    }

    @Test
    void testInvalidCommand() {
        String[] invalidArgs = {"invalid", "my-vpn"};
        assertNull(PARSER.parse(invalidArgs));
        String[] missingCommandArgs = {"my-vpn"};
        assertNull(PARSER.parse(missingCommandArgs));
        String[] tooManyArgs = {INIT_COMMAND, "-f", "my-vpn", "config.json", "extra"};
        assertNull(PARSER.parse(tooManyArgs));
        String[] tooManyArgs2 = {INIT_COMMAND, "my-vpn", "config.json", "extra"};
        assertNull(PARSER.parse(tooManyArgs2));
        String[] invertedArgs = {INIT_COMMAND, "my-vpn", "-f", "config.json",};
        assertNull(PARSER.parse(invertedArgs));
    }

    @Test
    void testMissingName() {
        String[] initArgs = {INIT_COMMAND, "config.json"};
        assertNull(PARSER.parse(initArgs));
        String[] deleteArgs = {DELETE_COMMAND};
        assertNull(PARSER.parse(deleteArgs));
    }

    @Test
    void testExtraOption() {
        String[] initArgs = {INIT_COMMAND, "my-vpn", "-x", "extra.json"};
        assertNull(PARSER.parse(initArgs));
        String[] startArgs = {START_COMMAND, "my-vpn", "-x"};
        assertNull(PARSER.parse(startArgs));
    }
}
