package com.fathzer.terravpn.providers;

import static com.fathzer.terravpn.Constants.*;

import java.util.Map;

import com.fathzer.terravpn.VPSProvider;
import com.fathzer.terravpn.utils.Registerable;

/**
 * DigitalOcean VPS provider implementation.
 * This provider allows deploying OpenVPN servers on DigitalOcean's cloud infrastructure.
 */

@Registerable(
        value = "digitalOcean",
        classes = {VPSProvider.class}
)
public class DigitalOceanVPS extends VPSProvider {
    @Override
    public String name() {
        return "DigitalOcean VPS";
    }

    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            INSTANCE_TYPE_VAR, "s-1vcpu-512mb-10gb",
            ZONE_VAR, "sf03"
        );
    }

    @Override
    public String getCompletedResource() {
        return "digitalocean_droplet.vpn";
    }

    @Override
    protected boolean isProtocolVariablesRequired() {
        return true;
    }
}
