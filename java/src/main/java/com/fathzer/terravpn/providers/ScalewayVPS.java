package com.fathzer.terravpn.providers;

import static com.fathzer.terravpn.Constants.*;

import java.util.Map;

import com.fathzer.terravpn.VPSProvider;
import com.fathzer.terravpn.utils.Registerable;

/**
 * Scaleway VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Scaleway's cloud infrastructure.
 */

@Registerable(
        value = "scaleway",
        classes = {VPSProvider.class}
)
public class ScalewayVPS extends VPSProvider {
    @Override
    public String name() {
        return "Scaleway VPS";
    }

    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            INSTANCE_TYPE_VAR, "DEV1-S",
            ZONE_VAR, "pl-waw-1",
            ROOT_VOLUME_SIZE_GB_VAR, 10
        );
    }

    @Override
    public String getCompletedResource() {
        return "scaleway_instance_server.vpn_server";
    }
}
