package com.fathzer.odvpn.providers.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;


import com.fathzer.odvpn.AbstractVPSProviderClient;

public abstract class BasicVPSProviderClient extends AbstractVPSProviderClient {

    protected BasicVPSProviderClient(String token) {
        super(new TokenAuthentication(token));
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
     * @see #getSSHKeyId(String, Class, Function)
     */
    public String getSSHKeyId(String keyName) throws IOException {
        return this.getSSHKeyId(keyName, SshKeysResponse.class, SshKeysResponse::sshKeys);
    }

    /**
     * Checks if an SSH key with the given name exists in the provider account.
     * @param keyName The name of the SSH key to check
     * @return The id of the SSH key if the key exists exactly once
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the key is unknown or duplicated
     */
    protected <T> String getSSHKeyId(String keyName, Class<T> responseType, Function<T, List<SshKey>> sshKeysGetter) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(getRootUrl() + getSshKeysPath())).build());
        final T keysResponse = this.objectMapper.readValue(response.body(), responseType);
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

    public void checkRegion(String region) throws IOException {
        checkRegion(region, RegionsResponse.class, r -> r.regions().stream().map(Region::name));
    }

    protected <T> void checkRegion(String region, Class<T> responseType, Function<T, Stream<String>> regionsGetter) throws IOException {
         final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(getRootUrl() + getRegionsPath())).build());
        final T locationsResponse = this.objectMapper.readValue(response.body(), responseType);
        if (regionsGetter.apply(locationsResponse).noneMatch(region::equals)) {
            throw new IllegalArgumentException("Unknown region " + region);
        }
    }

    protected String getInstanceTypesPath() {
        return "/instance-types";
    }

    public abstract void checkInstanceType(String region, String instanceType) throws IOException;

    protected <T> void checkInstanceType(String region, String instanceType, Class<T> responseType, Predicate<T> exists) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(getRootUrl() + getInstanceTypesPath())).build());
        final T instanceTypesResponse = this.objectMapper.readValue(response.body(), responseType);
        if (!exists.test(instanceTypesResponse)) {
            throw new IllegalArgumentException("Unknown instance type " + instanceType + " for region " + region);
        }
    }

}
