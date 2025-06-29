package com.fathzer.odvpn.providers;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.ObjectConfig;
import com.fathzer.odvpn.utils.Registerable;

/**
 * Permanent VPS provider implementation.
 * This provider allows deploying OpenVPN servers on a permanent VPS (One that is not managed by Terraform).
 */
@Registerable(
        value = "permanent",
        classes = {VPSProvider.class}
)
public class PermanentVPS extends VPSProvider {
    private static final String IP_ATTR = "ip";

    @Override
    public String name() {
        return "Permanent VPS";
    }

    @Override
    public List<String> checkConfiguration(ObjectConfig<VPSProvider> config) {
        final List<String> errors = new LinkedList<>();
        final String ip = config.config().get(IP_ATTR);
        if (ip == null) {
            errors.add("Missing IP");
        }
        return errors;
    }

    @Override
    public VPSState createVPS(InstanceParameters parameters, Consumer<VPSState> progress) {
        final String ip = parameters.vps().config().get(IP_ATTR);
        return new VPSState("permanentServer", ip, Status.READY);
    }

    @Override
    public boolean exists(InstanceParameters parameters, String id) {
        return true;
    }

    @Override
    public void deleteVPS(InstanceParameters parameters, String id) {
        // Do nothing, as the VPS is supposed to be permanent
    }
}
