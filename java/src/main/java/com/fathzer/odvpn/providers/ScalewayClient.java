package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.providers.utils.VPSCreationSettings;
import com.fathzer.odvpn.repository.VPNConfig;

public class ScalewayClient extends BasicVPSProviderClient {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SshKey(String id, String name, @JsonProperty("organization_id") String organizationId, @JsonProperty("project_id") String projectId, boolean disabled) {}

    private static final String API_URL = "https://api.scaleway.com";
    private static final Set<String> REGIONS;

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
        if (projectId!=null && !projectId.trim().isEmpty()) {
            final URI uri = URI.create(getRootUrl() + "/account/v3/projects/" + projectId);
            try {
                final String response = this.doRequest(this.newRequest(uri).build()).body();
                @JsonIgnoreProperties(ignoreUnknown = true)
                record Project(String id, String name, @JsonProperty("organization_id") String organizationId) {}
                final Project project = this.objectMapper.readValue(response, Project.class);
                if (project.id().equals(project.organizationId())) {
                    // Project is the default one
                    this.projectId = null;
                } else {
                    this.projectId = projectId;
                }
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
        } else {
            this.projectId = null;
        }
    }

    String getProjectId() {
        return this.projectId;
    }

    @Override
    protected String getRootUrl() {
        return API_URL;
    }

    private boolean matchesProjectId(SshKey sshKey) {
        if (this.projectId==null) {
            return sshKey.organizationId().equals(sshKey.projectId());
        } else {
            return this.projectId.equals(sshKey.projectId());
        }
    }

    @Override
    public String getSSHKeyId(String sshKey) throws IOException {
        final URI uri = URI.create(getRootUrl() + "/iam/v1alpha/ssh-keys");
        final String response = this.doRequest(this.newRequest(uri).build()).body();
        @JsonIgnoreProperties(ignoreUnknown = true)
        record SshKeyResponse(@JsonProperty("ssh_keys") List<SshKey> sshKeys) {}
        final List<SshKey> sshKeys = this.objectMapper.readValue(response, SshKeyResponse.class).sshKeys();
        List<SshKey> projectKeys = sshKeys.stream().filter(this::matchesProjectId).filter(s -> s.name().equals(sshKey)).toList();
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
        record IPCreationRequest(@JsonInclude(JsonInclude.Include.NON_NULL) String projectId, String type) {}
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
            @JsonProperty("routed_ip_enabled") boolean routedIpEnabled, @JsonProperty("ip_ids") List<String> ipIds,
            Map<String, Volume> volumes, String[] tags) {}
        final String[] tags = new String[] { "On-Demand-Vpn" };
        final InstanceCreationRequest request = new InstanceCreationRequest(
            this.projectId, settings.name(), settings.instanceType(), "41cce026-c90b-40cd-aead-a075a07196fb",
            true, List.of(ipId),
            Map.of("0", new Volume("l_ssd", "10000000000")), tags);
        final String response = this.post(URI.create(getRegionURI(settings.region()) + "/servers"), request);
        final String serverId = this.objectMapper.readTree(response).get("server").get("id").asText();

        // Return the server ID and the IP ID
        return serverId+"/"+settings.region()+"/"+ipId;
    }

    @Override
    public VPSState getState(String id) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(String id) throws IOException {
        final String serverId = id.split("/")[0];
        final String region = id.split("/")[1];
        final String ipId = id.split("/")[2];
        super.delete(serverId);
        this.doRequest(this.newRequest(URI.create(getIpsURI(region) + ipId)).DELETE().build());
    }
}
