package com.fathzer.terravpn.providers;

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
            "instance_type", "DEV1-S",
            "zone", "pl-waw-1",
            "root_volume_size_gb", 10
        );
    }

    @Override
    public String getCompletedResource() {
        return "scaleway_instance_server.vpn_server";
    }
}
