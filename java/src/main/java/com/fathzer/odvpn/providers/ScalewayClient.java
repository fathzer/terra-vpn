package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.providers.utils.VPSCreationSettings;
import com.fathzer.odvpn.repository.VPNConfig;

public class ScalewayClient extends BasicVPSProviderClient {
    @JsonIgnoreProperties(ignoreUnknown = true)
    static record SshKey(String id, String name, @JsonProperty("organization_id") String organizationId, @JsonProperty("project_id") String projectId, boolean disabled) {}

    static record VPSId(String serverId, String region, String ipId) {
        public static VPSId fromString(String id) {
            final String[] ids = id.split("/");
            return new VPSId(ids[0], ids[1], ids[2]);
        }
        public String toString() {
            return serverId + "/" + region + "/" + ipId;
        }
    }

    private static final String API_URL = "https://api.scaleway.com";
    private static final Set<String> REGIONS;

    private List<SshKey> sshKeys;
    private String projectId;

    static {
    	REGIONS = Arrays.stream(System.getProperty("scaleway.regions", "fr-par-1,fr-par-2,fr-par-3,nl-ams-1,nl-ams-2,nl-ams-3,pl-waw-1,pl-waw-2,pl-waw-3").split(",")).map(String::trim).collect(Collectors.toSet());
    }

    public ScalewayClient(String token) {
        super(b -> b.header("X-Auth-Token", token));
    }

    /**
     * Sets the project ID to use for the requests.
     * @param projectId the project ID to use
     * @throws IllegalArgumentException if the project ID is invalid or unknown
     * @throws IOException if an I/O error occurs
     */
    void setProjectId(String projectId) throws IOException {
        final String effectiveProjectId = projectId != null && projectId.trim().isEmpty() ? null : projectId;
        // Scaleway strange logic does not allow to get projects list without the organization id
        // But ssh keys do! So let use it to get the default project
        final URI uri = URI.create(getRootUrl() + "/iam/v1alpha1/ssh-keys");
        final String response = this.doRequest(this.newRequest(uri).build()).body();
        @JsonIgnoreProperties(ignoreUnknown = true)
        record SshKeyResponse(@JsonProperty("ssh_keys") List<SshKey> sshKeys) {}
        // Filter project's keys
        Predicate<SshKey> projectFilter = effectiveProjectId==null ? s -> s.organizationId().equals(s.projectId()) : s -> s.projectId().equals(effectiveProjectId);
        this.sshKeys = this.objectMapper.readValue(response, SshKeyResponse.class).sshKeys().stream().filter(projectFilter).toList();
        if (this.sshKeys.isEmpty()) {
            if (effectiveProjectId==null) {
                throw new IllegalArgumentException("Default project has no ssh keys");
            } else {
                this.checkProjectId(effectiveProjectId);
                this.projectId = effectiveProjectId;
            }
        } else {
            this.projectId = this.sshKeys.get(0).projectId();
        }
    }

    private void checkProjectId(String projectId) throws IOException {
        final URI projectUri = URI.create(getRootUrl() + "/account/v3/projects/" + projectId);
        try {
            this.doRequest(this.newRequest(projectUri).build());

        } catch (ErrorResponseException e) {
            String message;
            if (e.getStatusCode() == 404) {
                message = "Unknown project ID " + projectId;
            } else if (e.getStatusCode() == 400) {
                message = "Malformed project ID " + projectId;
            } else {
                message = e.getMessage();
            }
            throw new IllegalArgumentException(message, e);
        }
    }

    String getProjectId() {
        return this.projectId;
    }

    @Override
    protected String getRootUrl() {
        return API_URL;
    }

    @Override
    public String getSSHKeyId(String sshKey) throws IOException {
        List<SshKey> projectKeys = this.sshKeys.stream().filter(s -> s.name().equals(sshKey)).toList();
        if (projectKeys.isEmpty()) {
            throw new IllegalArgumentException("Unknown key");
        } else {
            projectKeys = projectKeys.stream().filter(s -> !s.disabled()).toList();
            if (projectKeys.isEmpty()) {
                throw new IllegalArgumentException("Disabled key");
            }
            if (projectKeys.size() > 1) {
                throw new IllegalArgumentException("Duplicated key");
            }
            return projectKeys.get(0).id();
        }
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
        final URI uri = URI.create(getRegionURI(region) + "/products/servers/availability?per_page=100");
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

    private String getIpsURI(String region) {
        return getRegionURI(region) + "/ips/";
    }

    @Override
    public String create(VPSCreationSettings settings, VPNConfig vpnConfig) throws IOException {
        // Create IP address
        record IPCreationRequest(String project, String type) {}
        //TODO Change address type to ipv4
        final String ipResponse = this.post(URI.create(getIpsURI(settings.region())), new IPCreationRequest(this.projectId, "routed_ipv6")); 
        final String ipId = this.objectMapper.readTree(ipResponse).get("ip").get("id").asText();

        // Create vps
        @JsonInclude(JsonInclude.Include.NON_NULL)
        record Volume(@JsonProperty("volume_type") String type, String size) {}
        @SuppressWarnings("java:S6218")
        record InstanceCreationRequest(
            @JsonInclude(JsonInclude.Include.NON_NULL) String project, String name,
            @JsonProperty("commercial_type") String instanceType, String image,
            @JsonProperty("dynamic_ip_required") boolean autoAllocatedIp, @JsonProperty("routed_ip_enabled") boolean routedIpEnabled, @JsonProperty("ip_ids") List<String> ipIds,
            Map<String, Volume> volumes, String[] tags) {}
        final String[] tags = new String[] { "On-Demand-Vpn" };
        final InstanceCreationRequest request = new InstanceCreationRequest(
            this.projectId, settings.name(), settings.instanceType(), "41cce026-c90b-40cd-aead-a075a07196fb",
            false, true, List.of(ipId),
            Map.of("0", new Volume("l_ssd", "10000000000")), tags);
        final String response = this.post(URI.create(getServerURI(settings.region())), request);
        final String serverId = this.objectMapper.readTree(response).get("server").get("id").asText();

        // Boot the VPS
        this.post(URI.create(getServerURI(settings.region(), serverId) + "/action"), Map.of("action", "poweron"));

        // Return the server ID and the IP ID
        return new VPSId(serverId, settings.region(), ipId).toString();
    }

    private String getServerURI(String region) {
        return getRegionURI(region) + "/servers";
    }

    private String getServerURI(String region, String serverId) {
        return getServerURI(region) + "/" + serverId;
    }

    @Override
    public VPSState getState(String id) throws IOException {
        final VPSId vpsId = VPSId.fromString(id);
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(getServerURI(vpsId.region(), vpsId.serverId()))).build());
System.out.println(response.body()); //TODO Remove
        final JsonNode server = this.objectMapper.readTree(response.body()).get("server");
        Status status = Status.STARTING;
        String ip = null;
        if (!server.isNull()) {
            final String scalewayStatus = server.get("state").asText();
            final JsonNode ipsNode = server.get("public_ips");
            for (JsonNode ipNode : ipsNode) {
                ip = ipNode.get("address").asText().trim();
                System.out.println("  -> " + ip); //TODO Remove
                if (ip.isEmpty()) {
                    ip = null;
                } else {
                    status = "running".equals(scalewayStatus) ? Status.READY : Status.IP_READY;
                }
            }
        }
        return new VPSState(id, ip, status);
    }

    @Override
    public void delete(String id) throws IOException {
        final VPSId vpsId = VPSId.fromString(id);
        this.doRequest(this.newRequest(URI.create(getServerURI(vpsId.region(), vpsId.serverId()))).DELETE().build());
        this.doRequest(this.newRequest(URI.create(getIpsURI(vpsId.region()) + vpsId.ipId())).DELETE().build());
    }
}
