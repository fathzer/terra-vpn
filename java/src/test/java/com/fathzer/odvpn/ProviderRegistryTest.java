package com.fathzer.odvpn;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.providers.AfraidDDNS;
import com.fathzer.odvpn.providers.DigitalOceanVPS;
import com.fathzer.odvpn.providers.OvhDDNS;
import com.fathzer.odvpn.providers.ScalewayVPS;

class ProviderRegistryTest {
    @Test
    void test() {
        assertEquals(AfraidDDNS.class, ProviderRegistry.getProvider("afraid", DynamicDNSProvider.class).getClass());
        assertEquals(OvhDDNS.class, ProviderRegistry.getProvider("ovh", DynamicDNSProvider.class).getClass());
        assertEquals(ScalewayVPS.class, ProviderRegistry.getProvider("scaleway", VPSProvider.class).getClass());
        assertEquals(DigitalOceanVPS.class, ProviderRegistry.getProvider("digitalOcean", VPSProvider.class).getClass());
    }
}
