package com.fathzer.terravpn.providers;

import static com.fathzer.terravpn.Constants.*;

import java.util.List;
import java.util.Map;

import com.fathzer.terravpn.VPSProvider;
import com.fathzer.terravpn.utils.Registerable;

/**
 * Vultr VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Vultr's cloud infrastructure.
 */
@Registerable(
        value = "vultr",
        classes = {VPSProvider.class}
)
public class VultrVPS extends VPSProvider {
    @Override
    public String name() {
        return "Vultr VPS";
    }

    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(
            INSTANCE_TYPE_VAR, "vc2-1c-0.5gb",
            ZONE_VAR, "ewr"
        );
    }

    @Override
    public String getCompletedResource() {
        return "vultr_instance.vpn";
    }


    @Override
    protected List<String> getExtraInitalizationCommands() {
        // We should install Docker
        return List.of("""
            if docker info >/dev/null 2>&1; then
                echo "Docker daemon is running"
            else
                echo "Docker is NOT running or not installed"
                sudo apt update
sudo apt install -y docker.io
            fi""");
    }
}
