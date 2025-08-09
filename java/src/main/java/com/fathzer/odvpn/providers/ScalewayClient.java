package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
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
