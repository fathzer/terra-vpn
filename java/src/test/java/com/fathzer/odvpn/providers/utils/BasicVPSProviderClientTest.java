package com.fathzer.odvpn.providers.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.http.Request;
import com.fathzer.http.RequestDecorator;
import com.fathzer.http.Response;

class BasicVPSProviderClientTest {

    // Test implementation of BasicVPSProviderClient
    private static class TestClient extends BasicVPSProviderClient {
        private String customSshKeysPath;
        private String customRegionsPath;
        
        public TestClient(String token) {
            super(RequestDecorator.bearerAuth(token));
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
    private Response<String> httpResponse;
    private TestClient testClient;
    private Request lastRequest;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        testClient = new TestClient("test-token");
        testClient.getRestClient().withRequestSender((c,r) -> {
        	lastRequest = r;
        	return httpResponse;
        });
        
        when(httpResponse.statusCode()).thenReturn(200);
        lastRequest = null;
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
        assertEquals("https://api.example.com/ssh-keys", lastRequest.getUri().toString());
        assertEquals("GET", lastRequest.getMethod().name());
        assertEquals(List.of("Bearer test-token"), lastRequest.getHeaders().get("Authorization"));

        // Test with unknown key
        assertThrows(IllegalArgumentException.class, () -> testClient.getSSHKeyId("nonexistent-key"));
        // Test with duplicate key
        assertThrows(IllegalArgumentException.class, () -> testClient.getSSHKeyId("another-key"));

        // Test with custom path
        lastRequest = null;
        testClient.customSshKeysPath = "/custom-keys";
        testClient.getSSHKeyId("test-key");
        assertEquals("https://api.example.com/custom-keys", lastRequest.getUri().toString());


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
        assertEquals("https://api.example.com/regions", lastRequest.getUri().toString());
        assertEquals("GET", lastRequest.getMethod().name());
        assertEquals(List.of("Bearer test-token"), lastRequest.getHeaders().get("Authorization"));


        // Unknown region
        assertThrows(IllegalArgumentException.class, () -> testClient.checkRegion("par2"));

        // Test with custom path
        lastRequest = null;
        testClient.customRegionsPath = "/custom-regions";
        testClient.checkRegion("lon1");
        assertEquals("https://api.example.com/custom-regions", lastRequest.getUri().toString());
        
        // Custom response type
        lastRequest = null;
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
        assertEquals("https://api.example.com/instance-types", lastRequest.getUri().toString());
        assertEquals("GET", lastRequest.getMethod().name());
        assertEquals(List.of("Bearer test-token"), lastRequest.getHeaders().get("Authorization"));

        // Clear invocations before next call
        // Unknown type
        assertThrows(IllegalArgumentException.class, () -> testClient.checkInstanceType("us-east-1", "x1.large"));
        // Custom response type
        when(httpResponse.body()).thenReturn("""
            { "types": ["t2.micro"] }
        """);
        @JsonIgnoreProperties(ignoreUnknown = true)
        record CustomTypesResponse(List<String> types) {}
        testClient.checkInstanceType("us-east-1", "t2.micro", CustomTypesResponse.class, resp -> resp.types() != null && resp.types().contains("t2.micro"));
    }
}
