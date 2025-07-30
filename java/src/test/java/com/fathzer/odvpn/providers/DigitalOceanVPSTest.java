package com.fathzer.odvpn.providers;

import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSSettings;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.ssh.Ssh;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DigitalOceanVPSTest {
    @Test
    void testName() {
        assertEquals("DigitalOcean VPS", new DigitalOceanVPS().name());
    }

    @Test
    void testConfigClass() {
        assertEquals(BasicTokenAuthVPSSettings.class, new DigitalOceanVPS().getConfigClass());
    }

    @Test
    void testDefaultRegionAndType() {
        DigitalOceanVPS vps = new DigitalOceanVPS();
        assertEquals("sfo3", vps.getDefaultRegion());
        assertEquals("s-1vcpu-512mb-10gb", vps.getDefaultInstanceType());
    }

    @Test
    void testGetClient() {
        DigitalOceanVPS vps = new DigitalOceanVPS();
        vps.setSettings(new BasicTokenAuthVPSSettings());
        BasicVPSProviderClient client = vps.getClient();
        assertNotNull(client);
    }

    @Test
    void testInitVPSDisablesUFW() throws IOException {
        DigitalOceanVPS vps = new DigitalOceanVPS();
        Ssh ssh = mock(Ssh.class);
        VPNConfig config = mock(VPNConfig.class);
        vps.initVPS(ssh, config);
        verify(ssh).exec(eq("sudo ufw disable"), any(OutputStream.class), any(OutputStream.class));
    }
}
