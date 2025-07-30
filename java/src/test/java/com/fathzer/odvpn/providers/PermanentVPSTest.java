package com.fathzer.odvpn.providers;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.repository.VPNConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PermanentVPSTest {
    private static void setField(Object obj, String field, Object value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    void testCheckConfiguration_missingIp() {
        PermanentVPS vps = new PermanentVPS();
        List<String> errors = vps.checkConfiguration();
        assertTrue(errors.contains("Missing IP"));
    }

    @Test
    void testCheckConfiguration_invalidIp() {
        PermanentVPS vps = new PermanentVPS();
        PermanentVPS.PermanentSettings settings = new PermanentVPS.PermanentSettings();
        setField(settings, "ip", "not.an.ip");
        vps.setSettings(settings);
        List<String> errors = vps.checkConfiguration();
        assertTrue(errors.get(0).startsWith("IP "));
    }

    @Test
    void testCheckConfiguration_validIp() {
        PermanentVPS vps = new PermanentVPS();
        PermanentVPS.PermanentSettings settings = new PermanentVPS.PermanentSettings();
        setField(settings, "ip", "1.2.3.4");
        vps.setSettings(settings);
        List<String> errors = vps.checkConfiguration();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testCreateVPS() {
        PermanentVPS vps = new PermanentVPS();
        PermanentVPS.PermanentSettings settings = new PermanentVPS.PermanentSettings();
        setField(settings, "ip", "1.2.3.4");
        vps.setSettings(settings);
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
        PermanentVPS.PermanentSettings settings = new PermanentVPS.PermanentSettings();
        setField(settings, "ip", "1.2.3.4");
        setField(settings, "sshUser", "customuser");
        vps.setSettings(settings);
        assertEquals("customuser", vps.getSSHUser());
        setField(settings, "sshUser", null);
        assertEquals("root", vps.getSSHUser());
    }
}
