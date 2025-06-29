package com.fathzer.odvpn.providers;

import java.util.List;
import java.util.function.Consumer;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.ObjectConfig;
import com.fathzer.odvpn.utils.Registerable;

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
    public List<String> checkConfiguration(ObjectConfig<VPSProvider> config) {
        return List.of();
    }

    @Override
    public VPSState createVPS(InstanceParameters parameters, Consumer<VPSState> progress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(InstanceParameters parameters, String id) {
        return false;
    }

    @Override
    public void deleteVPS(InstanceParameters parameters, String id) {
        throw new UnsupportedOperationException();
    }
}
