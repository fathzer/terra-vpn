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
import com.fathzer.odvpn.providers.VultrClient.InstanceCreationRequest;
import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSSettings;
import com.fathzer.odvpn.utils.Registerable;

/**
 * Vultr VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Vultr's cloud infrastructure.
 */
@Registerable(
        value = "vultr",
        classes = {VPSProvider.class}
)
public class VultrVPS extends VPSProvider<BasicTokenAuthVPSSettings> {
    private static final Logger logger = LoggerFactory.getLogger(VultrVPS.class);

    private static final String DEFAULT_INSTANCE_TYPE = "vc2-1c-0.5gb";
    private static final String DEFAULT_REGION = "ewr";

    
    @Override
    public String name() {
        return "Vultr VPS";
    }

    @Override
    public Class<BasicTokenAuthVPSSettings> getConfigClass() {
        return BasicTokenAuthVPSSettings.class;
    }

    @Override
    public List<String> checkConfiguration() throws IOException {
        List<String> errors = new LinkedList<>();
        String token = getToken();
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

        try (VultrClient client = new VultrClient(token)) {
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
            // Check if instance type exists in the region
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
        try (VultrClient client = new VultrClient(getToken())) {
            final String sshKeyId = client.getSSHKeyId(resolve(settings.getSshKeyName()));
            final String instanceName = getInstanceName();
            InstanceCreationRequest request = new InstanceCreationRequest(
                settings.getRegion(DEFAULT_REGION),
                settings.getInstanceType(DEFAULT_INSTANCE_TYPE),
                instanceName,
                "docker", "disabled", List.of("On demand VPN"), List.of(sshKeyId));
            final String id = client.create(request);
            logger.info("Vultr instance created with ID {} start waiting for it to be ready", id);
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
            logger.info("Vultr instance {} is ready", id);
            return state;
        }
    }

    @Override
    public boolean exists(String id) throws IOException {
        try (VultrClient client = new VultrClient(getToken())) {
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
        try (VultrClient client = new VultrClient(getToken())) {
            client.delete(id);
            logger.info("Vultr instance {} deleted", id);
        }
    }

    private String getToken() {
        return resolve(settings.getToken());
    }
}
