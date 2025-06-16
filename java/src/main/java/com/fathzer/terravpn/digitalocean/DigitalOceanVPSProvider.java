package com.fathzer.terravpn.digitalocean;

import java.util.Map;
import java.util.Optional;

import com.fathzer.terravpn.VPSProvider;

/**
 * Scaleway VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Scaleway's cloud infrastructure.
 */

public class DigitalOceanVPSProvider implements VPSProvider {
    @Override
    public String id() {
        return "digitalOceanVps";
    }
    @Override
    public String name() {
        return "DigitalOcean VPS";
    }
    @Override
    public Optional<String> description() {
        return Optional.of("DigitalOcean cloud infrastructure provider for VPS instances");
    }

    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            "vps_instance_type", "s-1vcpu-1gb",
            "vps_zone", "lon1"
        );
    }

    @Override
    public String getCompletedResource() {
        return "digitalocean_droplet.vpn";
    }
}
