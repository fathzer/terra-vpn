package com.fathzer.terravpn.scaleway;

import java.util.Map;
import java.util.Optional;

import com.fathzer.terravpn.VPSProvider;

/**
 * Scaleway VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Scaleway's cloud infrastructure.
 */

public class ScalewayVPSProvider implements VPSProvider {
    @Override
    public String id() {
        return "scalewayVps";
    }
    @Override
    public String name() {
        return "Scaleway";
    }
    @Override
    public Optional<String> description() {
        return Optional.of("Scaleway cloud infrastructure provider for VPS instances");
    }

    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            "vps_instance_type", "DEV1-S",
            "vps_zone", "pl-waw-1",
            "vps_root_volume_size_gb", 10
        );
    }

    @Override
    public String getCompletedResource() {
        return "scaleway_instance_server.vpn_server";
    }
}
