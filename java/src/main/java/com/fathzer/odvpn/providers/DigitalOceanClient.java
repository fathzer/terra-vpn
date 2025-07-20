package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

import com.fathzer.odvpn.providers.utils.VPSCreationSettings;
import com.fathzer.odvpn.utils.IOLambdas.IOFunction;

public class DigitalOceanClient extends BasicVPSProviderClient {
    private static final String API_URL = "https://api.digitalocean.com/v2";

    public DigitalOceanClient(String token) {
        super(token);
    }

    @Override
    protected String getRootUrl() {
        return API_URL;
    }

    @Override
    protected String getSshKeysPath() {
        return "/account/keys";
    }

    @Override
    protected String getRegionsPath() {
        return super.getRegionsPath()+"?per_page=200";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Region(String slug, boolean available, List<String> sizes) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RegionsResponse(@JsonProperty("regions") List<Region> regions) {}

    @Override
    public void checkRegion(String region) throws IOException {
        checkRegion(region, response -> this.objectMapper.readValue(response, RegionsResponse.class).regions().stream().filter(r -> r.available).map(Region::slug));
    }

    @Override
    protected String getInstanceTypesPath() {
        return getRegionsPath();
    }

    public void checkInstanceType(String region, String instanceType) throws IOException {
        checkInstanceType(region, instanceType, RegionsResponse.class, s -> s.regions().stream().anyMatch(r -> r.available() && r.slug().equals(region) && r.sizes().contains(instanceType)));
    }

    @Override
    protected String getInstancesPath() {
        return "/servers";
    }

    public String create(VPSCreationSettings settings) throws IOException {
        IOFunction<String, String> idGetter = response -> this.objectMapper.readTree(response).get("server").get("id").asText();
        record InstanceCreationRequest(
            String name,
            String location,
            @JsonProperty("server_type") String serverType,
            String image,
            @JsonProperty("ssh_keys") List<String> sshKeyIds,
            Map<String, String> labels) {}
        return create(settings, s->new InstanceCreationRequest(
            s.name(), s.region(), s.instanceType(),
            "docker-ce", List.of(s.sshKeyId()), Map.of("application", "On_demand_VPN")), idGetter);
    }

    @Override
    public VPSState getState(String id) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(getRootUrl() + getInstancesPath() + "/" + id)).build());
        final JsonNode server = this.objectMapper.readTree(response.body()).get("server");
        Status status = Status.STARTING;
        String ip = null;
        if (!server.isNull()) {
            final String hetznerStatus = server.get("status").asText();
            final JsonNode ipNode = server.path("public_net").path("ipv4");
            if (!ipNode.isNull()) {
                ip = ipNode.get("ip").asText().trim();
                if (ip.isEmpty()) {
                	ip = null;
                } else {
                	status = "running".equals(hetznerStatus) ? Status.READY : Status.IP_READY;
                }
            }
        }
        return new VPSState(id, ip, status);
    }
}