package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.AbstractVPSProviderClient.AuthenticationException;
import com.fathzer.odvpn.AbstractVPSProviderClient.ErrorResponseException;
import com.fathzer.odvpn.providers.HetznerClient.InstanceCreationRequest;
import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSConfiguration;
import com.fathzer.odvpn.utils.Registerable;

/**
 * Hetzner VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Hetzner's cloud infrastructure.
 */
@Registerable(
        value = "hetzner",
        classes = {VPSProvider.class}
)
public class HetznerVPS extends VPSProvider<BasicTokenAuthVPSConfiguration> {
    private static final Logger logger = LoggerFactory.getLogger(HetznerVPS.class);

    private static final String DEFAULT_INSTANCE_TYPE = "cpx11";
    private static final String DEFAULT_REGION = "nbg1";
    
    @Override
    public String name() {
        return "Hetzner VPS";
    }

    @Override
    public Class<BasicTokenAuthVPSConfiguration> getConfigClass() {
        return BasicTokenAuthVPSConfiguration.class;
    }

    @Override
    public List<String> checkConfiguration() throws IOException {
        List<String> errors = new LinkedList<>();
        String token = settings.getToken();
        if (token == null) {
            errors.add("Missing token");
        }
        String sshKeyName = settings.getSshKeyName();
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
            // Check if region exists
            String region = settings.getRegion(DEFAULT_REGION);
            try {
                client.checkRegion(region);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
            // Check if instance type exists in the location
            try {
                client.checkInstanceType(region, settings.getInstanceType(DEFAULT_INSTANCE_TYPE));
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public VPSState createVPS(Consumer<VPSState> progress) throws IOException {
        try (HetznerClient client = new HetznerClient(settings.getToken())) {
            final String sshKeyId = client.getSSHKeyId(settings.getSshKeyName());
            final String instanceName = getInstanceName();
            InstanceCreationRequest request = new InstanceCreationRequest(
                settings.getRegion(DEFAULT_REGION),
                settings.getInstanceType(DEFAULT_INSTANCE_TYPE),
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
    public boolean exists(String id) throws IOException {
        try (HetznerClient client = new HetznerClient(settings.getToken())) {
            return !client.getState(id).status().equals(Status.STOPPED);
        } catch (ErrorResponseException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void deleteVPS(String id) throws IOException {
        try (HetznerClient client = new HetznerClient(settings.getToken())) {
            client.delete(id);
            logger.info("Hetzner instance {} deleted", id);
        }
    }
}
