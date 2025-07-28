package com.fathzer.odvpn.providers.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fathzer.odvpn.AbstractVPSProviderClient;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.utils.IOLambdas.IOFunction;

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
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(getRootUrl() + getSshKeysPath())).build());
        final String keysResponse = response.body();
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
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(getRootUrl() + getRegionsPath())).build());
        if (regionsGetter.apply(response.body()).noneMatch(region::equals)) {
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

    protected String getInstancesPath() {
        return "/instances";
    }

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
        return idGetter.apply(this.post(URI.create(getRootUrl() + getInstancesPath()), bodyRequestBuilder.apply(request)));
    }

    /**
     * Sends a POST request to the specified URI with the given body.
     * @param uri the URI to send the request to
     * @param body the body of the request
     * @return the response body
     * @throws IOException if an I/O error occurs
     */
    protected String post(URI uri, Object body) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(uri).
            POST(HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(body))).
            header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
            build());
        return response.body();
    }

    public abstract VPSState getState(String id) throws IOException;
    
    public void delete(String id) throws IOException {
        this.doRequest(this.newRequest(URI.create(getRootUrl() + getInstancesPath() + "/" + id)).DELETE().build());
    }
}
