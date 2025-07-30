package com.fathzer.odvpn.providers;

import com.fathzer.odvpn.providers.PermanentVPS.PermanentSettings;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.repository.VPNConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PermanentVPSTest {

    @Test
    void testCheckConfiguration() {
        // No settings
        PermanentVPS vps = new PermanentVPS();
        assertTrue(vps.checkConfiguration().contains("Missing IP"));

        // Invalid IP
        vps.setSettings(new PermanentSettings("not.an.ip", null));
        assertTrue(vps.checkConfiguration().get(0).startsWith("IP "));

        // Valid IP
        vps.setSettings(new PermanentSettings("1.2.3.4", "sshKey"));
        assertTrue(vps.checkConfiguration().isEmpty());
    }

    @Test
    void testCreateVPS() {
        PermanentVPS vps = new PermanentVPS();
        vps.setSettings(new PermanentSettings("1.2.3.4", "sshKey"));
        VPSProvider.VPSState state = vps.createVPS(new VPNConfig("host.domain.com", List.of(), null, 0), s -> {});
        assertEquals("permanentServer", state.id());
        assertEquals("1.2.3.4", state.ip());
        assertEquals(VPSProvider.Status.READY, state.status());
    }

    @Test
    void testExistsAlwaysTrue() {
        PermanentVPS vps = new PermanentVPS();
        assertTrue(vps.exists("any"));
    }

    @Test
    void testDeleteVPSDoesNotThrow() {
        PermanentVPS vps = new PermanentVPS();
        assertDoesNotThrow(() -> vps.deleteVPS("irrelevant"));
    }

    @Test
    void testGetSSHUser_customAndDefault() {
        PermanentVPS vps = new PermanentVPS();
        vps.setSettings(new PermanentSettings("1.2.3.4", "customuser"));
        assertEquals("customuser", vps.getSSHUser());
        vps.setSettings(new PermanentSettings("1.2.3.4", null));
        assertEquals("root", vps.getSSHUser());
    }
}
