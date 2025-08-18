package com.fathzer.odvpn.providers;

import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSSettings;
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
        final VultrVPS vps = new VultrVPS();
        final BasicTokenAuthVPSSettings settings = new BasicTokenAuthVPSSettings();
        settings.setToken("token");
		vps.setSettings(settings);
        assertNotNull(vps.getClient());
    }
}
