package com.fathzer.odvpn.providers.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.odvpn.AbstractVPSProviderClient.AuthenticationException;
import com.fathzer.odvpn.AbstractVPSProviderClient.ErrorResponseException;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.VPSProvider;

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
            String region = getDefaultRegion();
            try {
                client.checkRegion(region);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
            // Check if instance type exists in the region
            try {
                client.checkInstanceType(region, getDefaultInstanceType());
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public VPSState createVPS(VPNConfig vpnConfig, Consumer<VPSState> progress) throws IOException {
        try (BasicVPSProviderClient client = getClient()) {
            final String sshKeyId = client.getSSHKeyId(getSshKeyName());
            final String id = client.create(new VPSCreationSettings(getInstanceName(), getDefaultRegion(),
                getDefaultInstanceType(), sshKeyId), vpnConfig);
            if (logger.isInfoEnabled()) logger.info("{} instance created with ID {} start waiting for it to be ready", this.name(), id);
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
            if (logger.isInfoEnabled()) logger.info("{} instance {} is ready", this.name(), id);
            return state;
        }
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
    
