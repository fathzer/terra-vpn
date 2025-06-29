package com.fathzer.odvpn;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CommandTest {
    
    private static final Path CONFIG = Path.of("config.json");

    @Test
    void testValidCommand() {
        Command command = new Command("init", "my-vpn", CONFIG, false);
        assertEquals("init", command.command());
        assertEquals("my-vpn", command.name());
        assertEquals(CONFIG, command.configPath());
        assertFalse(command.force());

        command = new Command("start", "my-vpn", null, false);
        assertNull(command.configPath());

        command = new Command("init", "my-vpn", CONFIG, true);
        assertTrue(command.force());
    }

    @Test
    void testIllegalArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Command(null, "my-vpn", CONFIG, false));
        assertThrows(IllegalArgumentException.class, () -> new Command("", "my-vpn", CONFIG, false));
        assertThrows(IllegalArgumentException.class, () -> new Command(" ", "my-vpn", CONFIG, false));
        assertThrows(IllegalArgumentException.class, () -> new Command("init", null, CONFIG, false));
        assertThrows(IllegalArgumentException.class, () -> new Command("init", "", CONFIG, false));
        assertThrows(IllegalArgumentException.class, () -> new Command("init", " ", CONFIG, false));
    }
}
