package com.fathzer.odvpn.providers;

import java.util.List;
import java.util.function.Consumer;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSConfiguration;
import com.fathzer.odvpn.utils.Registerable;

/**
 * Scaleway VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Scaleway's cloud infrastructure.
 */
@Registerable(
        value = "scaleway",
        classes = {VPSProvider.class}
)
public class ScalewayVPS extends VPSProvider<BasicTokenAuthVPSConfiguration> {
    @Override
    public String name() {
        return "Scaleway VPS";
    }

    @Override
    public Class<BasicTokenAuthVPSConfiguration> getConfigClass() {
        return BasicTokenAuthVPSConfiguration.class;
    }

    @Override
    public List<String> checkConfiguration() {
        return List.of();
    }

    @Override
    public VPSState createVPS(Consumer<VPSState> progress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String id) {
        return false;
    }

    @Override
    public void deleteVPS(String id) {
        throw new UnsupportedOperationException();
    }
}
