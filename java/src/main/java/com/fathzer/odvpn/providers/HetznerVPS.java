package com.fathzer.odvpn.providers;

import static com.fathzer.odvpn.Constants.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.AbstractVPSProviderClient.AuthenticationException;
import com.fathzer.odvpn.AbstractVPSProviderClient.ErrorResponseException;
import com.fathzer.odvpn.providers.HetznerClient.InstanceCreationRequest;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.ObjectConfig;
import com.fathzer.odvpn.utils.Registerable;

/**
 * Vultr VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Vultr's cloud infrastructure.
 */
@Registerable(
        value = "hetzner",
        classes = {VPSProvider.class}
)
public class HetznerVPS extends VPSProvider {
    private static final Logger logger = LoggerFactory.getLogger(HetznerVPS.class);

    private static final String DEFAULT_INSTANCE_TYPE = "cpx11";
    private static final String DEFAULT_ZONE = "nbg1";

    private static final String TOKEN_VAR = "token";
    private static final String SSH_KEY_NAME_VAR = "ssh_key_name";
    
    @Override
    public String name() {
        return "Hetzner VPS";
    }

    @Override
    public List<String> checkConfiguration(ObjectConfig<VPSProvider> config) throws IOException {
        List<String> errors = new LinkedList<>();
        String token = config.config().get(TOKEN_VAR);
        if (token == null) {
            errors.add("Missing token");
        }
        String sshKeyName = config.config().get(SSH_KEY_NAME_VAR);
        if (sshKeyName == null || sshKeyName.trim().isEmpty()) {
            errors.add("Missing SSH key name or SSH key name is empty");
        }
        if (!errors.isEmpty()) {
            return errors;
        }

        try (HetznerClient client = new HetznerClient(token)) {
            // Check if the SSH key exists
            try {
                client.getSSHKeyId(sshKeyName);
            } catch (AuthenticationException e) {
                errors.add(e.getMessage());
                return errors;
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
            // Check if zone exists
            String zone = config.config().getOrDefault(ZONE_VAR, DEFAULT_ZONE);
            try {
                client.checkZone(zone);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
            // Check if instance type exists in the zone
            try {
                client.checkInstanceType(zone, config.config().getOrDefault(INSTANCE_TYPE_VAR, DEFAULT_INSTANCE_TYPE));
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public VPSState createVPS(InstanceParameters parameters, Consumer<VPSState> progress) throws IOException {
        Map<String, String> config = parameters.vps().config();
        try (HetznerClient client = new HetznerClient(config.get(TOKEN_VAR))) {
            final String sshKeyId = client.getSSHKeyId(config.get(SSH_KEY_NAME_VAR));
            final String instanceName = getInstanceName();
            InstanceCreationRequest request = new InstanceCreationRequest(
                config.getOrDefault(ZONE_VAR, DEFAULT_ZONE),
                config.getOrDefault(INSTANCE_TYPE_VAR, DEFAULT_INSTANCE_TYPE),
                instanceName,
                "docker", "disabled", List.of("On demand VPN"), List.of(sshKeyId));
            final String id = client.create(request);
            logger.info("Hetzner instance created with ID {} start waiting for it to be ready", id);
            VPSState state;
            for (state=client.getState(id); !state.status().equals(Status.READY); state=client.getState(id)) {
                if (!state.status().equals(Status.IP_READY)) {
                    progress.accept(state);
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
            }
            logger.info("Hetzner instance {} is ready", id);
            return state;
        }
    }

    @Override
    public boolean exists(InstanceParameters parameters, String id) throws IOException {
        try (HetznerClient client = new HetznerClient(parameters.vps().config().get(TOKEN_VAR))) {
            return !client.getState(id).status().equals(Status.STOPPED);
        } catch (ErrorResponseException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void deleteVPS(InstanceParameters parameters, String id) throws IOException {
        try (HetznerClient client = new HetznerClient(parameters.vps().config().get(TOKEN_VAR))) {
            client.delete(id);
            logger.info("Hetzner instance {} deleted", id);
        }
    }
}
