package com.fathzer.odvpn.providers.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.providers.utils.AbstractVPSProviderClient.AuthenticationException;
import com.fathzer.odvpn.providers.utils.AbstractVPSProviderClient.ErrorResponseException;

public abstract class BasicVPSProvider<T extends BasicTokenAuthVPSSettings> extends VPSProvider<T> {
    private static final Logger logger = LoggerFactory.getLogger(BasicVPSProvider.class);

    protected abstract BasicVPSProviderClient getClient();

    @SuppressWarnings("unchecked")
    @Override
    public Class<T> getConfigClass() {
        return (Class<T>) BasicTokenAuthVPSSettings.class;
    }

    protected String getToken() {
        return resolve(settings.getToken());
    }

    protected String getSshKeyName() {
        return resolve(settings.getSshKeyName());
    }

    protected String getRegion() {
        return settings.getRegion(getDefaultRegion());
    }

    protected String getInstanceType() {
        return settings.getInstanceType(getDefaultInstanceType());
    }

    protected abstract String getDefaultRegion();

    protected abstract String getDefaultInstanceType();

    @Override
    public List<String> checkConfiguration() throws IOException {
        List<String> errors = new LinkedList<>();
        String token = getToken();
        if (token == null) {
            errors.add("Missing token");
        }
        String sshKeyName = getSshKeyName();
        if (sshKeyName == null || sshKeyName.trim().isEmpty()) {
            errors.add("Missing SSH key name or SSH key name is empty");
        }
        if (!errors.isEmpty()) {
            return errors;
        }

        try (BasicVPSProviderClient client = getClient()) {
            errors.addAll(doExtraCheck(client));
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
            String region = getRegion();
            try {
                client.checkRegion(region);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
            // Check if instance type exists in the region
            try {
                client.checkInstanceType(region, getInstanceType());
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    /** Performs additional checks on the configuration.
     * <br>This method is called by {@link #checkConfiguration()} after the basic checks (token, SSH key name are not null or empty) and before any call to the client.
     * @param client the client to use
     * @return A list of configuration errors, or an empty list if the configuration is valid
     * @throws IOException if an I/O error occurs
     */
    protected List<String> doExtraCheck(BasicVPSProviderClient client) throws IOException {
        return new LinkedList<>();
    }

    @Override
    public VPSState createVPS(VPNConfig vpnConfig, Consumer<VPSState> progress) throws IOException {
        try (BasicVPSProviderClient client = getClient()) {
            final String sshKeyId = client.getSSHKeyId(getSshKeyName());
            final String id = client.create(new VPSCreationSettings(getInstanceName(), settings.getRegion(getDefaultRegion()),
                settings.getInstanceType(getDefaultInstanceType()), sshKeyId), vpnConfig);
            if (logger.isInfoEnabled()) logger.info("{} instance created with ID {} start waiting for it to be ready", this.name(), id);
            VPSState state;
            for (state = client.getState(id); !Status.READY.equals(state.status()); state=client.getState(id)) {
                if (!Status.IP_READY.equals(state.status())) {
                    progress.accept(state);
                }
                try {
                    Thread.sleep(getReadyWaitFrequencyMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
            }
            if (logger.isInfoEnabled()) logger.info("{} instance {} is ready", this.name(), id);
            return state;
        }
    }
    
    long getReadyWaitFrequencyMs() {
    	return 3000;
    }

    @Override
    public boolean exists(String id) throws IOException {
        try (BasicVPSProviderClient client = getClient()) {
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
        try (BasicVPSProviderClient client = getClient()) {
            client.delete(id);
            if (logger.isInfoEnabled()) logger.info("{} instance {} deleted", this.name(), id);
        }
    }
}
    
