package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.util.LinkedList;
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
    public List<String> checkConfiguration() throws IOException {
        final List<String> errors = new LinkedList<>();
        try {
            ((ScalewayClient) getClient()).setProjectId(settings.getProjectId());
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }
        errors.addAll(super.checkConfiguration());
        return errors;
    }
}
