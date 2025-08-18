package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fathzer.http.Request;
import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

import com.fathzer.odvpn.providers.utils.VPSCreationSettings;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.utils.IOLambdas.IOFunction;

public class HetznerClient extends BasicVPSProviderClient {
    private static final String API_URL = "https://api.hetzner.cloud/v1";

    public HetznerClient(String token) {
        super(token);
    }

    @Override
    protected String getRootUrl() {
        return API_URL;
    }

    @Override
    protected String getSshKeysPath() {
        return "/ssh_keys";
    }

    @Override
    protected String getRegionsPath() {
        return "/locations";
    }

    @Override
    public void checkRegion(String region) throws IOException {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Region(String name) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record LocationsResponse(@JsonProperty("locations") List<Region> regions) {}
        checkRegion(region, response -> this.objectMapper.readValue(response, LocationsResponse.class).regions().stream().map(Region::name));
    }

    @Override
    protected String getInstanceTypesPath() {
        return "/server_types";
    }

    public void checkInstanceType(String region, String instanceType) throws IOException {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Price(String location) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record ServerType(String name, List<Price> prices) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record ServerTypesResponse(@JsonProperty("server_types") List<ServerType> serverTypes) {
            public boolean exists(String region, String instanceType) {
                final Optional<ServerType> serverType = serverTypes.stream().filter(s -> s.name.equals(instanceType)).findAny();
                return serverType.isPresent() && serverType.get().prices.stream().anyMatch(p -> p.location.equals(region));
            }
        }
        checkInstanceType(region, instanceType, ServerTypesResponse.class, s -> s.exists(region, instanceType));
    }

    @Override
    protected String getInstancesPath() {
        return "/servers";
    }

    @Override
    public String create(VPSCreationSettings settings, VPNConfig vpnConfig) throws IOException {
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
            "docker-ce", List.of(s.sshKeyId()), Map.of("application", "On-Demand-VPN")), idGetter);
    }

    @Override
    public VPSState getState(String id) throws IOException {
        final String response = this.execute(new Request(getRootUrl() + getInstancesPath() + "/" + id));
        final JsonNode server = this.objectMapper.readTree(response).get("server");
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