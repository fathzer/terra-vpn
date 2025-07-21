package com.fathzer.odvpn.providers;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSSettings;
import com.fathzer.odvpn.providers.utils.BasicVPSProvider;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.utils.Registerable;

/**
 * DigitalOcean VPS provider implementation.
 * This provider allows deploying OpenVPN servers on DigitalOcean's cloud infrastructure.
 */
@Registerable(
    value = "digitalOcean",
    classes = {VPSProvider.class}
)
public class DigitalOceanVPS extends BasicVPSProvider<BasicTokenAuthVPSSettings> {
    private static final String DEFAULT_REGION = "sfo3";
    private static final String DEFAULT_INSTANCE_TYPE = "s-1vcpu-512mb-10gb";
    
    @Override
    public String name() {
        return "DigitalOcean VPS";
    }

    @Override
    public Class<BasicTokenAuthVPSSettings> getConfigClass() {
        return BasicTokenAuthVPSSettings.class;
    }

    @Override
    protected BasicVPSProviderClient getClient() {
        return new DigitalOceanClient(getToken());
    }

    @Override
    protected String getDefaultRegion() {
        return DEFAULT_REGION;
    }

        protected String getDefaultInstanceType() {
        return DEFAULT_INSTANCE_TYPE;
    }
}
