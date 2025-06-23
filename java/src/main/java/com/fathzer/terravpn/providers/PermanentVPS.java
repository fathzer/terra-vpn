package com.fathzer.terravpn.providers;

import java.util.Map;

import com.fathzer.terravpn.Constants;
import com.fathzer.terravpn.VPSProvider;
import com.fathzer.terravpn.utils.Registerable;

/**
 * Permanent VPS provider implementation.
 * This provider allows deploying OpenVPN servers on a permanent VPS (One that is not managed by Terraform).
 */

@Registerable(
        value = "permanent",
        classes = {VPSProvider.class}
)
public class PermanentVPS extends VPSProvider {
    @Override
    public String name() {
        return "Permanent VPS";
    }

    @Override
    public String getCompletedResource() {
        return "";
    }

    @Override
    public Map<String, Object> getDefaultConfig() {
        return Map.of(Constants.INSTANCE_TYPE_VAR, "", Constants.ZONE_VAR, "", Constants.ROOT_VOLUME_SIZE_GB_VAR, 10);
    }
}
