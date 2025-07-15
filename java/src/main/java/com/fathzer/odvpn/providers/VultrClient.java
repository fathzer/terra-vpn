package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

public class VultrClient extends BasicVPSProviderClient {
    private static final String API_URL = "https://api.vultr.com/v2";

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorResponse(String error, int status) {}

    record InstanceCreationRequest(String region,
        String plan,
        String label,
        @JsonProperty("image_id") String imageId,
        String backups,
        List<String> tags,
        @JsonProperty("sshkey_id") List<String> sshkeyIds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InstanceFullResponse(InstanceResponse instance) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InstanceResponse(String id,
        @JsonProperty("main_ip") String mainIp,
        @JsonProperty("power_status") String powerStatus,
        @JsonProperty("server_status") String serverStatus) {

        public String mainIp() {
            if (mainIp != null && !mainIp.trim().isEmpty()) {
                return mainIp.trim();
            } else {
                return null;
            }
        }
    }

    public VultrClient(String token) {
        super(token);
    }

    @Override
    protected String getRootUrl() {
        return API_URL;
    }

    @Override
    protected String getInstanceTypesPath() {
        return "/plans";
    }
    public void checkInstanceType(String region, String instanceType) throws IOException {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Plan(String id, List<String> locations) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record PlansResponse(List<Plan> plans) {
            public boolean exists(String region, String instanceType) {
                return plans.stream().anyMatch(plan -> plan.id.equals(instanceType) && plan.locations.contains(region));
            }
        }
        checkInstanceType(region, instanceType, PlansResponse.class, s -> s.exists(region, instanceType));
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
        return new AuthenticationException(response.statusCode(), this.getErrorMessage(response));
    }

    @Override
    protected ErrorResponseException getErrorResponseException(HttpResponse<String> response) throws IOException {
        return new ErrorResponseException(response.statusCode(), this.getErrorMessage(response));
    }

    @Override
    protected ServerErrorException getServerErrorException(HttpResponse<String> response) throws IOException {
        return new ServerErrorException(response.statusCode(), this.getErrorMessage(response));
    }

    String create(InstanceCreationRequest request) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/instances")).POST(HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(request))).build());
        final InstanceResponse instanceResponse = this.objectMapper.readValue(response.body(), InstanceFullResponse.class).instance();
        return instanceResponse.id;
    }

    VPSState getState(String id) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/instances/" + id)).build());
        final InstanceResponse instanceResponse = this.objectMapper.readValue(response.body(), InstanceFullResponse.class).instance();
        final Status status;
        if (instanceResponse==null || instanceResponse.mainIp()==null) {
            status = Status.STARTING;
        } else if ("running".equals(instanceResponse.powerStatus()) && "ok".equals(instanceResponse.serverStatus())) {
            status = Status.READY;
        } else {
            status = Status.IP_READY;
        }
        return new VPSState(id, instanceResponse==null ? null : instanceResponse.mainIp(), status);
    }

    void delete(String id) throws IOException {
        this.doRequest(this.newRequest(URI.create(API_URL + "/instances/" + id)).DELETE().build());
    }
}
