package com.fathzer.odvpn.providers.utils;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fathzer.http.Request;
import com.fathzer.http.RequestDecorator;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.utils.IOLambdas.IOFunction;

public abstract class BasicVPSProviderClient extends AbstractVPSProviderClient {

    protected BasicVPSProviderClient(RequestDecorator authentication) {
        super(authentication);
    }

    protected BasicVPSProviderClient(String token) {
        super(RequestDecorator.bearerAuth(token));
    }

    protected abstract String getRootUrl();

    protected String getSshKeysPath() {
        return "/ssh-keys";
    }

     /**
     * Checks if an SSH key with the given name exists in the provider account.
     * <br>This default implementation is based on the SSHKeysResponse class.
     * @param keyName The name of the SSH key to check
     * @return The id of the SSH key if the key exists exactly once
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the key is unknown or duplicated
     * @see #getSSHKeyId(String, IOFunction)
     */
    public String getSSHKeyId(String keyName) throws IOException {
        return this.getSSHKeyId(keyName, r -> this.objectMapper.readValue(r, SshKeysResponse.class).sshKeys());
    }

    /**
     * Checks if an SSH key with the given name exists in the provider account.
     * @param keyName The name of the SSH key to check
     * @return The id of the SSH key if the key exists exactly once
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the key is unknown or duplicated
     */
    protected String getSSHKeyId(String keyName, IOFunction<String, List<SshKey>> sshKeysGetter) throws IOException {
        final String keysResponse = this.execute(new Request(getRootUrl() + getSshKeysPath()));
        // Filter keys by name (case-sensitive)
        final List<SshKey> matchingKeys = sshKeysGetter.apply(keysResponse).stream()
            .filter(key -> keyName.equals(key.name()))
            .toList();
        if (matchingKeys.isEmpty()) {
            throw new IllegalArgumentException("Unknown key");
        } else if (matchingKeys.size() > 1) {
            throw new IllegalArgumentException("Duplicated key");
        }
        return matchingKeys.get(0).id();
    }

    protected String getRegionsPath() {
        return "/regions";
    }

    public abstract void checkRegion(String region) throws IOException ;

    protected void checkRegion(String region, IOFunction<String, Stream<String>> regionsGetter) throws IOException {
        final String response = this.execute(new Request(getRootUrl() + getRegionsPath()));
        if (regionsGetter.apply(response).noneMatch(region::equals)) {
            throw new IllegalArgumentException("Unknown region " + region);
        }
    }

    protected String getInstanceTypesPath() {
        return "/instance-types";
    }

    public abstract void checkInstanceType(String region, String instanceType) throws IOException;

    protected <T> void checkInstanceType(String region, String instanceType, Class<T> responseType, Predicate<T> exists) throws IOException {
        final String response = this.execute(new Request(getRootUrl() + getInstanceTypesPath()));
        final T instanceTypesResponse = this.objectMapper.readValue(response, responseType);
        if (!exists.test(instanceTypesResponse)) {
            throw new IllegalArgumentException("Unknown instance type " + instanceType + " for region " + region);
        }
    }

    protected String getInstancesPath() {
        return "/instances";
    }

    /**
     * Creates a new instance.
     * @param settings the settings for the instance
     * @param vpnConfig the VPN configuration (could be usefull to apply firewall rules to allow only ssh and vpn ports)
     * @return the instance ID
     * @throws IOException if an I/O error occurs
     */
    public abstract String create(VPSCreationSettings settings, VPNConfig vpnConfig) throws IOException;

    /**
     * Creates a new instance.
     * @param request the request containing the instance settings
     * @param bodyRequestBuilder a function that builds the http request body from the request settings
     * @param idGetter a function that extracts the instance ID from the response body
     * @return the instance ID
     * @throws IOException if an I/O error occurs
     */
    protected <T> String create(VPSCreationSettings request, Function<VPSCreationSettings, T> bodyRequestBuilder, IOFunction<String, String> idGetter) throws IOException {
        return idGetter.apply(this.execute(new Request(getRootUrl() + getInstancesPath()).post(bodyRequestBuilder.apply(request))));
    }

    public abstract VPSState getState(String id) throws IOException;
    
    public void delete(String id) throws IOException {
        this.execute(new Request(getRootUrl() + getInstancesPath() + "/" + id).delete());
    }
}
