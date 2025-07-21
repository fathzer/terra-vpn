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
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.utils.IOLambdas.IOFunction;

public class DigitalOceanClient extends BasicVPSProviderClient {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Region(String slug, boolean available, List<String> sizes) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RegionsResponse(@JsonProperty("regions") List<Region> regions) {}
    private record FirewallSource(List<String> addresses) {}
    private record FirewallRule(String protocol, String ports, List<FirewallSource> sources) {}

    private static final String API_URL = "https://api.digitalocean.com/v2";

    private static final String FIREWALLS_PATH = "/firewalls";
    private static final FirewallRule SSH_RULE = new FirewallRule("tcp", "22", List.of(new FirewallSource(List.of("0.0.0.0/0", "::/0"))));

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
        return "/droplets";
    }

    @Override
    public String create(VPSCreationSettings settings, VPNConfig config) throws IOException {
        // First create the droplet
        IOFunction<String, String> idGetter = response -> this.objectMapper.readTree(response).get("droplet").get("id").asText();
        record InstanceCreationRequest(
            String name,
            String region,
            String size,
            String image,
            @JsonProperty("ssh_keys") List<String> sshKeyIds,
            Map<String, String> tags) {}
        final String serverId = create(settings, s->new InstanceCreationRequest(s.name(), s.region(), s.instanceType(),
            "docker-20-04", List.of(s.sshKeyId()), Map.of("application", "On-Demand-Vpn")), idGetter);
        // Then create the firewall and adds the droplet to it
        final FirewallRule vpnRule = new FirewallRule(config.protocol().name().toLowerCase(), Integer.toString(config.port()), List.of(new FirewallSource(List.of("0.0.0.0/0", "::/0"))));
        record FirewallCreationRequest(
            String name,
            @JsonProperty("droplet_ids") List<String> dropletIds,
            @JsonProperty("inbound_rules") List<FirewallRule> rules) {}
        final FirewallCreationRequest firewallCreationRequest = new FirewallCreationRequest("On-Demand-Vpn", List.of(serverId), List.of(SSH_RULE, vpnRule));
        final String firewallResponseBody = this.post(URI.create(getRootUrl() + FIREWALLS_PATH), firewallCreationRequest);
        final String firewallId = this.objectMapper.readTree(firewallResponseBody).get("firewall").get("id").asText();
        return serverId+"/"+firewallId;
    }

    @Override
    public VPSState getState(String id) throws IOException {
        final String serverId = id.split("/")[0];
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(getRootUrl() + getInstancesPath() + "/" + serverId)).build());
        final JsonNode server = this.objectMapper.readTree(response.body()).get("droplet");
        Status status = Status.STARTING;
        String ip = null;
        if (!server.isNull()) {
            final String doStatus = server.get("status").asText();
            final JsonNode ipNode = server.path("networks").path("v4").get(0);
            if (!ipNode.isNull()) {
                ip = ipNode.get("ip_address").asText().trim();
                if (ip.isEmpty()) {
                	ip = null;
                } else {
                	status = "active".equals(doStatus) ? Status.READY : Status.IP_READY;
                }
            }
        }
        return new VPSState(id, ip, status);
    }

    @Override
    public void delete(String id) throws IOException {
        final String serverId = id.split("/")[0];
        final String firewallId = id.split("/")[1];
        super.delete(serverId);
        this.doRequest(this.newRequest(URI.create(getRootUrl() + FIREWALLS_PATH + "/" + firewallId)).DELETE().build());
    }
}