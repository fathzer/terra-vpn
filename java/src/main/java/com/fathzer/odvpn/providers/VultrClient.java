package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.providers.utils.VPSCreationSettings;
import com.fathzer.odvpn.utils.IOLambdas.IOFunction;

public class VultrClient extends BasicVPSProviderClient {
    private static final String API_URL = "https://api.vultr.com/v2";

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InstanceFullResponse(InstanceResponse instance) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InstanceResponse(String id,
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
    public void checkRegion(String region) throws IOException {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Region(String id) {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        record RegionsResponse(List<Region> regions) {}
        checkRegion(region, response -> this.objectMapper.readValue(response, RegionsResponse.class).regions().stream().map(Region::id));
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

    @Override
    protected String getErrorMessage(HttpResponse<String> response) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record ErrorResponse(String error, int status) {}
        try {
            final ErrorResponse errorResponse = this.objectMapper.readValue(response.body(), ErrorResponse.class);
            return errorResponse.error;
        } catch (IOException e) {
            return "Unknown error with " + response.statusCode()+ " status code";
        }
    }

    @Override
    public String create(VPSCreationSettings request) throws IOException {
        IOFunction<String, String> idGetter = response -> this.objectMapper.readValue(response, InstanceFullResponse.class).instance().id;
        record InstanceCreationRequest(String region, String plan, String label,
            @JsonProperty("image_id") String imageId, String backups, List<String> tags,
            @JsonProperty("sshkey_id") List<String> sshkeyIds) {}

        return create(request, s->new InstanceCreationRequest(
            s.region(), s.instanceType(), s.name(), "docker-ce", "no", List.of("application"), List.of(s.sshKeyId())), idGetter);
    }

    @Override
    public VPSState getState(String id) throws IOException {
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
}
