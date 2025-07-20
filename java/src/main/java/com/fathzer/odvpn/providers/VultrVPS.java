package com.fathzer.odvpn.providers;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSSettings;
import com.fathzer.odvpn.providers.utils.BasicVPSProvider;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.utils.Registerable;

/**
 * Vultr VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Vultr's cloud infrastructure.
 */
@Registerable(
        value = "vultr",
        classes = {VPSProvider.class}
)
public class VultrVPS extends BasicVPSProvider<BasicTokenAuthVPSSettings> {
    private static final String DEFAULT_INSTANCE_TYPE = "vc2-1c-0.5gb";
    private static final String DEFAULT_REGION = "ewr";

    @Override
    public String name() {
        return "Vultr VPS";
    }

    @Override
    public Class<BasicTokenAuthVPSSettings> getConfigClass() {
        return BasicTokenAuthVPSSettings.class;
    }

    protected BasicVPSProviderClient getClient() {
        return new VultrClient(getToken());
    }

    protected String getDefaultRegion() {
        return DEFAULT_REGION;
    }

    protected String getDefaultInstanceType() {
        return DEFAULT_INSTANCE_TYPE;
    }
}
