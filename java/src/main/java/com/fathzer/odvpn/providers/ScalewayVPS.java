package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.util.List;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.providers.utils.BasicVPSProvider;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.utils.Registerable;

/**
 * Scaleway VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Scaleway's cloud infrastructure.
 */
@Registerable(
        value = "scaleway",
        classes = {VPSProvider.class}
)
public class ScalewayVPS extends BasicVPSProvider<ScalewaySettings> {
    @Override
    public String name() {
        return "Scaleway VPS";
    }

    @Override
    public Class<ScalewaySettings> getConfigClass() {
        return ScalewaySettings.class;
    }

    @Override
    protected BasicVPSProviderClient getClient() {
        return new ScalewayClient(getToken());
    }

    @Override
    protected String getDefaultRegion() {
        return "pl-waw-2";
    }

    @Override
    protected String getDefaultInstanceType() {
        return "STARDUST1-S";
    }

    @Override
    protected void prepare(BasicVPSProviderClient client, Operation operation) throws IOException {
        if (operation == Operation.CREATE) {
            ((ScalewayClient) client).setProjectId(getSettings().getProjectId());
        }
    }

    @Override
    protected List<String> doExtraCheck(BasicVPSProviderClient client) throws IOException {
        try {
            ((ScalewayClient) client).setProjectId(getSettings().getProjectId());
            return List.of();
        } catch (IllegalArgumentException e) {
            return List.of(e.getMessage());
        }
    }
}
