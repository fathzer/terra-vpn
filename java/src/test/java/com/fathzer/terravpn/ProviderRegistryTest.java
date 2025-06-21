package com.fathzer.terravpn;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fathzer.terravpn.providers.AfraidDDNS;
import com.fathzer.terravpn.providers.DigitalOceanVPS;
import com.fathzer.terravpn.providers.OvhDDNS;
import com.fathzer.terravpn.providers.ScalewayVPS;

class ProviderRegistryTest {
    @Test
    void test() {
        assertEquals(AfraidDDNS.class, ProviderRegistry.getProvider("afraid", DynamicDNSProvider.class).getClass());
        assertEquals(OvhDDNS.class, ProviderRegistry.getProvider("ovh", DynamicDNSProvider.class).getClass());
        assertEquals(ScalewayVPS.class, ProviderRegistry.getProvider("scaleway", VPSProvider.class).getClass());
        assertEquals(DigitalOceanVPS.class, ProviderRegistry.getProvider("digitalOcean", VPSProvider.class).getClass());
    }
}
