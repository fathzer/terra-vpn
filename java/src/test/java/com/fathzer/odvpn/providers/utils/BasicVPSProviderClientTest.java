package com.fathzer.odvpn.providers.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.AbstractVPSProviderClient;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.repository.VPNConfig;

class BasicVPSProviderClientTest {

    // Test implementation of BasicVPSProviderClient
    private static class TestClient extends BasicVPSProviderClient {
        private String customSshKeysPath;
        private String customRegionsPath;
        
        public TestClient(String token) {
            super(token);
        }

        @Override
        protected String getRootUrl() {
            return "https://api.example.com";
        }
            
        @Override
        protected String getSshKeysPath() {
            return customSshKeysPath != null ? customSshKeysPath : super.getSshKeysPath();
        }
            
        @Override
        protected String getRegionsPath() {
            return customRegionsPath != null ? customRegionsPath : super.getRegionsPath();
        }
            
        public void setHttpClient(HttpClient client) {
            try {
                java.lang.reflect.Field clientField = AbstractVPSProviderClient.class.getDeclaredField("client");
                clientField.setAccessible(true);
                clientField.set(this, client);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set HTTP client", e);
            }
        }

        @Override
        public void checkRegion(String region) throws IOException {
            @JsonIgnoreProperties(ignoreUnknown = true)
            record Region(String name) {}
            @JsonIgnoreProperties(ignoreUnknown = true)
            record LocationsResponse(List<Region> regions) {}
            checkRegion(region, r -> this.objectMapper().readValue(r, LocationsResponse.class).regions().stream().map(Region::name));
        }

        @Override
        public void checkInstanceType(String region, String instanceType) throws IOException {
            // Default response type: { "instance_types": ["t2.micro", "c2.medium"] }
            @JsonIgnoreProperties(ignoreUnknown = true)
            record InstanceTypesResponse(java.util.List<String> instance_types) {}
            checkInstanceType(region, instanceType, InstanceTypesResponse.class,
                resp -> resp.instance_types() != null && resp.instance_types().contains(instanceType));
        }

        @Override
        public String create(VPSCreationSettings request, VPNConfig vpnConfig) throws IOException {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public VPSState getState(String id) throws IOException {
            throw new UnsupportedOperationException("Not implemented");
        }

        private ObjectMapper objectMapper() {
            return this.objectMapper;
        }
    }
    
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;
    private TestClient testClient;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        testClient = new TestClient("test-token");
        testClient.setHttpClient(httpClient);
        
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.<String>send(any(), any())).thenReturn(httpResponse);
    }

    @Test
    void testGetSshKeyId() throws Exception {
        when(httpResponse.body()).thenReturn("""
            {
                "ssh_keys": [
                    {"id": "key1", "name": "test-key", "expires": "2025-01-01"},
                    {"id": "key2", "name": "another-key"},
                    {"id": "key3", "name": "another-key"}
                ]
            }
            """);
        String keyId = testClient.getSSHKeyId("test-key");
        assertEquals("key1", keyId);
        
        verify(httpClient).send(argThat(req -> {
            HttpRequest request = (HttpRequest) req;
            return request.uri().toString().equals("https://api.example.com/ssh-keys") &&
                   request.method().equals("GET") &&
                   request.headers().firstValue("Authorization").orElse("").equals("Bearer test-token");
        }), any());

        // Test with unknown key
        assertThrows(IllegalArgumentException.class, () -> testClient.getSSHKeyId("nonexistent-key"));
        // Test with duplicate key
        assertThrows(IllegalArgumentException.class, () -> testClient.getSSHKeyId("another-key"));

        // Test with custom path
        testClient.customSshKeysPath = "/custom-keys";
        testClient.getSSHKeyId("test-key");
        verify(httpClient).send(argThat(req -> {
            HttpRequest request = (HttpRequest) req;
            return request.uri().toString().equals("https://api.example.com/custom-keys");
        }), any());

        // Test with custom response type
        when(httpResponse.body()).thenReturn("""
            { "keys": [{"id": "key1", "name": "test-key"}] }
        """);
        record TestSshKeysResponse(List<SshKey> keys) {}
        keyId = testClient.getSSHKeyId("test-key", r -> testClient.objectMapper().readValue(r, TestSshKeysResponse.class).keys());
        assertEquals("key1", keyId);
    }
    
    @Test
    void testCheckRegion() throws Exception {
        // Given
        when(httpResponse.body()).thenReturn("""
            {
                "regions": [
                    {"name": "nyc1"},
                    {"name": "sgp1"},
                    {"name": "lon1"}
                ]
            }
            """);
        
        // Known region
        testClient.checkRegion("sgp1");
        
        // Verify the request was made correctly
        verify(httpClient).send(argThat(req -> {
            HttpRequest request = (HttpRequest) req;
            return request.uri().toString().equals("https://api.example.com/regions") &&
                   request.method().equals("GET") &&
                   request.headers().firstValue("Authorization").orElse("").equals("Bearer test-token");
        }), any());

        // Unknown region
        assertThrows(IllegalArgumentException.class, () -> testClient.checkRegion("par2"));

        // Test with custom path
        testClient.customRegionsPath = "/custom-regions";
        testClient.checkRegion("lon1");
        verify(httpClient).send(argThat(req -> {
            HttpRequest request = (HttpRequest) req;
            return request.uri().toString().equals("https://api.example.com/custom-regions");
        }), any());
        
        // Custom response type
        when(httpResponse.body()).thenReturn("""
            { "locations": ["nyc1", "sgp1"] }
        """);
        record CustomRegionsResponse(List<String> locations) {}
        testClient.checkRegion("sgp1", r -> testClient.objectMapper().readValue(r, CustomRegionsResponse.class).locations().stream());
    }

    @Test
    void testCheckInstanceType() throws Exception {
        // Mock HTTP response for instance types
        when(httpResponse.body()).thenReturn("""
            { "instance_types": ["t2.micro", "c2.medium"] }
        """);
        // Known type
        testClient.checkInstanceType("us-east-1", "t2.micro");
        // Verify request path
        verify(httpClient).send(argThat(req -> {
            HttpRequest request = (HttpRequest) req;
            return request.uri().toString().equals("https://api.example.com/instance-types");
        }), any());
        // Clear invocations before next call
        clearInvocations(httpClient);
        // Unknown type
        assertThrows(IllegalArgumentException.class, () -> testClient.checkInstanceType("us-east-1", "x1.large"));
        // Custom response type
        when(httpResponse.body()).thenReturn("""
            { "types": ["t2.micro"] }
        """);
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        record CustomTypesResponse(java.util.List<String> types) {}
        testClient.checkInstanceType("us-east-1", "t2.micro", CustomTypesResponse.class, resp -> resp.types() != null && resp.types().contains("t2.micro"));
    }
}
