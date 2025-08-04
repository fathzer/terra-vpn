package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.providers.utils.VPSCreationSettings;
import com.fathzer.odvpn.repository.VPNConfig;

public class ScalewayClient extends BasicVPSProviderClient {
    private static final String API_URL = "https://api.scaleway.com";

    private static final Set<String> REGIONS;

    static {
    	REGIONS = Arrays.stream(System.getProperty("scaleway.regions", "fr-par-1,fr-par-2,fr-par-3,nl-ams-1,nl-ams-2,nl-ams-3,pl-waw-1,pl-waw-2,pl-waw-3").split(",")).map(String::trim).collect(Collectors.toSet());
    }

    public ScalewayClient(String token) {
        super(b -> b.header("X-Auth-Token", token));
    }

    @Override
    protected String getRootUrl() {
        return API_URL;
    }

    @Override
    protected String getSshKeysPath() {
        return "/iam/v1alpha1/ssh-keys";
    }

    @Override
    public void checkRegion(String region) throws IOException {
        if (!REGIONS.contains(region)) {
            throw new IllegalArgumentException("Invalid region: " + region);
        }
    }

    @Override
    public void checkInstanceType(String region, String instanceType) throws IOException {
        this.checkRegion(region);
        final URI uri = URI.create(getRegionURI(region) + "products/servers/availability?per_page=100");
        final String response = this.doRequest(this.newRequest(uri).build()).body();
        @JsonIgnoreProperties(ignoreUnknown = true)
        record AvailabilityResponse() {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record ServerType(@JsonProperty("servers") Map<String, AvailabilityResponse> instanceTypes) {}
        final Map<String, AvailabilityResponse> instanceTypesMap = this.objectMapper.readValue(response, ServerType.class).instanceTypes();
        if (!instanceTypesMap.containsKey(instanceType)) {
            throw new IllegalArgumentException("Unknown instance type " + instanceType + " for region " + region);
        }
    }

    private String getRegionURI(String region) {
        return API_URL + "/instance/v1/zones/" + region;
    }

    @Override
    public String create(VPSCreationSettings settings, VPNConfig vpnConfig) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VPSState getState(String id) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}
