package com.fathzer.terravpn.digitalocean;

import java.util.Map;
import java.util.Optional;

import com.fathzer.terravpn.VPSProvider;

/**
 * Scaleway VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Scaleway's cloud infrastructure.
 */

public class DigitalOceanVPSProvider extends VPSProvider {
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
            "vps_instance_type", "s-1vcpu-512mb-10gb",
            "vps_zone", "sf03"
        );
    }

    @Override
    public String getCompletedResource() {
        return "digitalocean_droplet.vpn";
    }
}
