package com.fathzer.odvpn.providers;

import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSSettings;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VultrVPSTest {
    @Test
    void testName() {
        assertEquals("Vultr VPS", new VultrVPS().name());
    }

    @Test
    void testConfigClass() {
        assertEquals(BasicTokenAuthVPSSettings.class, new VultrVPS().getConfigClass());
    }

    @Test
    void testDefaultRegionAndType() {
        VultrVPS vps = new VultrVPS();
        assertEquals("ewr", vps.getDefaultRegion());
        assertEquals("vc2-1c-0.5gb", vps.getDefaultInstanceType());
    }

    @Test
    void testGetClient() {
        VultrVPS vps = new VultrVPS();
        vps.setSettings(new BasicTokenAuthVPSSettings());
        BasicVPSProviderClient client = vps.getClient();
        assertNotNull(client);
    }
}
