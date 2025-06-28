package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fathzer.odvpn.AbstractVPSProviderClient;
import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;

class VultrClient extends AbstractVPSProviderClient {
    private static final String API_URL = "https://api.vultr.com/v2";
    private final String token;

    VultrClient(String token) {
        super();
        this.token = token;
    }

    @Override
    protected Builder newRequest(URI uri) {
        return super.newRequest(uri).header("Authorization", "Bearer " + this.token);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorResponse(@JsonProperty("error") String error,
        @JsonProperty("status") int status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SshKey(@JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("ssh_key") String key) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SshKeysResponse(@JsonProperty("ssh_keys") List<SshKey> sshKeys) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Region(@JsonProperty("id") String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RegionsResponse(@JsonProperty("regions") List<Region> regions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Plan(@JsonProperty("id") String id, 
        @JsonProperty("locations") List<String> locations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlansResponse(@JsonProperty("plans") List<Plan> plans) {}

    record InstanceCreationRequest(@JsonProperty("region") String region,
        @JsonProperty("plan") String plan,
        @JsonProperty("label") String label,
        @JsonProperty("image_id") String imageId,
        @JsonProperty("backups") String backups,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("sshkey_id") List<String> sshkeyIds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InstanceResponse(@JsonProperty("id") String id,
        @JsonProperty("main_ip") String mainIp,
        @JsonProperty("power_status") String powerStatus,
        @JsonProperty("server_status") String serverStatus) {}

    /**
     * Checks if an SSH key with the given name exists in the Vultr account.
     * @param keyName The name of the SSH key to check
     * @return An empty Optional if the key exists exactly once, "Unknown key" if not found,
     *         or "Duplicated key" if multiple keys with the same name exist
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the key is unknown or duplicated
     */
    String getSSHKeyId(String keyName) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/ssh-keys")).build());
        final SshKeysResponse keysResponse = this.objectMapper.readValue(response.body(), SshKeysResponse.class);
        // Filter keys by name (case-sensitive)
        final List<SshKey> matchingKeys = keysResponse.sshKeys.stream()
            .filter(key -> keyName.equals(key.name))
            .toList();
        if (matchingKeys.isEmpty()) {
            throw new IllegalArgumentException("Unknown key");
        } else if (matchingKeys.size() > 1) {
            throw new IllegalArgumentException("Duplicated key");
        }
        return matchingKeys.get(0).id;
    }

    void checkZone(String zone) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/regions")).build());
        final RegionsResponse regionsResponse = this.objectMapper.readValue(response.body(), RegionsResponse.class);
        if (regionsResponse.regions().stream().map(Region::id).noneMatch(zone::equals)) {
            throw new IllegalArgumentException("Unknown zone " + zone);
        }
    }

    void checkInstanceType(String zone, String instanceType) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/plans")).build());
        final PlansResponse plansResponse = this.objectMapper.readValue(response.body(), PlansResponse.class);
        boolean exists = plansResponse.plans.stream()
            .anyMatch(plan -> plan.id.equals(instanceType) && plan.locations.contains(zone));
        if (!exists) {
            throw new IllegalArgumentException("Unknown instance type " + instanceType + " for zone " + zone);
        }
    }

    private String getErrorMessage(HttpResponse<String> response) {
        try {
            final ErrorResponse errorResponse = this.objectMapper.readValue(response.body(), ErrorResponse.class);
            return errorResponse.error;
        } catch (IOException e) {
            return "Unknown error with " + response.statusCode()+ " status code";
        }
    }

    @Override
    protected AuthenticationException getAuthenticationException(HttpResponse<String> response) throws IOException {
        return new AuthenticationException(this.getErrorMessage(response));
    }

    @Override
    protected ErrorResponseException getErrorResponseException(HttpResponse<String> response) throws IOException {
        return new ErrorResponseException(this.getErrorMessage(response));
    }

    @Override
    protected ServerErrorException getServerErrorException(HttpResponse<String> response) throws IOException {
        return new ServerErrorException(this.getErrorMessage(response));
    }

    String create(InstanceCreationRequest request) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/instances")).POST(HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(request))).build());
        final InstanceResponse instanceResponse = this.objectMapper.readValue(response.body(), InstanceResponse.class);
        return instanceResponse.id;
    }

    VPSState getState(String id) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/instances/" + id)).build());
        final InstanceResponse instanceResponse = this.objectMapper.readValue(response.body(), InstanceResponse.class);
        final Status status;
        if (instanceResponse.powerStatus().equals("running") && instanceResponse.serverStatus().equals("ok")) {
            status = Status.READY;
        } else if (instanceResponse.mainIp() != null && !instanceResponse.mainIp().trim().isEmpty()) {
            status = Status.IP_READY;
        } else {
            status = Status.STARTING;
        }
        return new VPSState(id, instanceResponse.mainIp(), status);
    }

    void delete(String id) throws IOException {
        this.doRequest(this.newRequest(URI.create(API_URL + "/instances/" + id)).DELETE().build());
    }

    public static void main(String[] args) throws IOException {
        try (VultrClient client = new VultrClient("CVGDI64367A6IEQ2UEH5QZBHZ5DVBQ7T5GYA")) {
            System.out.println(client.getSSHKeyId("terra-vpn"));
            client.checkZone("mad");
            client.checkInstanceType("mad", "vc2-1c-1gb");
        }
    }
}
