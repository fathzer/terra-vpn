package com.fathzer.odvpn.providers;

import static com.fathzer.odvpn.json.VPSProviderTestUtils.deserialize;
import static org.junit.jupiter.api.Assertions.*;

import com.fathzer.odvpn.providers.PermanentVPS.PermanentSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.repository.VPNConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

class PermanentVPSTest {

    @Test
    void testCheckConfiguration() {
        // No settings
        PermanentVPS vps = new PermanentVPS();
        assertEquals(List.of("Missing IP"), vps.checkConfiguration());

        // Null IP
        vps.setSettings(new PermanentSettings(null, null));
        assertEquals(List.of("Missing IP"), vps.checkConfiguration());

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
    void testGetSSHUserCustomAndDefault() {
        PermanentVPS vps = new PermanentVPS();
        vps.setSettings(new PermanentSettings("1.2.3.4", "customuser"));
        assertEquals("customuser", vps.getSSHUser());
        vps.setSettings(new PermanentSettings("1.2.3.4", null));
        assertEquals("root", vps.getSSHUser());
    }

    @Test
    void testConfigDeserialization() throws IOException {
    	ObjectMapper mapper = new ObjectMapper();
        String json = """
        {
            "ip": "1.2.3.4",
            "sshUser": "customuser"
        }
        """;
        PermanentSettings settings = mapper.readValue(json, PermanentSettings.class);
        assertEquals("1.2.3.4", settings.ip());
        assertEquals("customuser", settings.sshUser());

        json = """
            {
                "ip": "1.2.3.4"
            }
            """;
        settings = mapper.readValue(json, PermanentSettings.class);
        assertEquals("1.2.3.4", settings.ip());

        json = """
            {
            "providerId": "permanent",
            "config": {
                "ip": "1.2.3.4"
                }
            }
            """;
        PermanentVPS vps2 = deserialize(mapper, json);
        assertEquals("1.2.3.4", vps2.getSettings().ip());
    }
}
