package com.fathzer.odvpn.providers;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSSettings;
import com.fathzer.odvpn.providers.utils.BasicVPSProvider;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.utils.Registerable;

/**
 * Hetzner VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Hetzner's cloud infrastructure.
 */
@Registerable(
        value = "hetzner",
        classes = {VPSProvider.class}
)
public class HetznerVPS extends BasicVPSProvider<BasicTokenAuthVPSSettings> {
    private static final String DEFAULT_INSTANCE_TYPE = "cx22";
    private static final String DEFAULT_REGION = "nbg1";
    
    @Override
    public String name() {
        return "Hetzner VPS";
    }

    @Override
    protected BasicVPSProviderClient getClient() {
        return new HetznerClient(getToken());
    }   

    @Override
    protected String getDefaultRegion() {
        return DEFAULT_REGION;
    }

    @Override
    protected String getDefaultInstanceType() {
        return DEFAULT_INSTANCE_TYPE;
    }
}
