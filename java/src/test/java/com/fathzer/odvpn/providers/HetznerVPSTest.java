package com.fathzer.odvpn.providers;

import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSSettings;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HetznerVPSTest {
    @Test
    void testName() {
        assertEquals("Hetzner VPS", new HetznerVPS().name());
    }

    @Test
    void testConfigClass() {
        assertEquals(BasicTokenAuthVPSSettings.class, new HetznerVPS().getConfigClass());
    }

    @Test
    void testDefaultRegionAndType() {
        HetznerVPS vps = new HetznerVPS();
        assertEquals("nbg1", vps.getDefaultRegion());
        assertEquals("cx22", vps.getDefaultInstanceType());
    }

    @Test
    void testGetClient() {
        HetznerVPS vps = new HetznerVPS();
        vps.setSettings(new BasicTokenAuthVPSSettings());
        BasicVPSProviderClient client = vps.getClient();
        assertNotNull(client);
    }
}
