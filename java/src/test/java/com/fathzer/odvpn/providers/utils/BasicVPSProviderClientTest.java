package com.fathzer.odvpn.providers.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fathzer.odvpn.AbstractVPSProviderClient;

class BasicVPSProviderClientTest {

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
        when(httpResponse.body()).thenReturn("""
            {
                "ssh_keys": [
                    {"id": "key1", "name": "test-key", "expires": "2025-01-01"},
                    {"id": "key2", "name": "another-key"}
                ]
            }
            """);
        when(httpClient.<String>send(any(), any())).thenReturn(httpResponse);
    }

    @Test
    void testGetSshKeyId() throws Exception {
        String keyId = testClient.getSSHKeyId("test-key");
        assertEquals("key1", keyId);
        
        verify(httpClient).send(argThat(req -> {
            HttpRequest request = (HttpRequest) req;
            return request.uri().toString().equals("https://api.example.com/ssh-keys") &&
                   request.method().equals("GET") &&
                   request.headers().firstValue("Authorization").orElse("").equals("Bearer test-token");
        }), any());
    }
    
    @Test
    void testGetSshKeyId_UnknownKey() {
        assertThrows(IllegalArgumentException.class, () -> testClient.getSSHKeyId("nonexistent-key"));
    }
    
    @Test
    void testGetSshKeyId_DuplicateKey() {
        when(httpResponse.body()).thenReturn("""
            {
                "ssh_keys": [
                    {"id": "key1", "name": "duplicate-key"},
                    {"id": "key2", "name": "duplicate-key"}
                ]
            }
            """);
        assertThrows(IllegalArgumentException.class, () -> testClient.getSSHKeyId("duplicate-key"));
    }
    
    @Test
    void testGetSshKeyId_WithCustomResponseType() throws Exception {
        String keyId = testClient.getSSHKeyId("test-key", TestSshKeysResponse.class, TestSshKeysResponse::sshKeys);
        assertEquals("key1", keyId);
    }
    
    @Test
    void testGetSshKeysPath() {
        try (TestClient testClient2 = new TestClient("token", "/custom-keys")) {
            assertEquals("/custom-keys", testClient2.getSshKeysPath());
        }
    }
    
    // Test implementation of BasicVPSProviderClient
    private static class TestClient extends BasicVPSProviderClient {
        private final String customSshKeysPath;
        
        public TestClient(String token) {
            this(token, null);
        }
        
        public TestClient(String token, String customSshKeysPath) {
            super(token);
            this.customSshKeysPath = customSshKeysPath;
        }
        
        @Override
        protected String getRootUrl() {
            return "https://api.example.com";
        }
        
        @Override
        protected String getSshKeysPath() {
            return customSshKeysPath != null ? customSshKeysPath : super.getSshKeysPath();
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
    }
    
    // Test implementation of SshKeysResponse
    public static class TestSshKeysResponse {
        private List<SshKey> sshKeys;
        
        // Default constructor for Jackson
        public TestSshKeysResponse() {}
        
        public TestSshKeysResponse(List<SshKey> sshKeys) {
            this.sshKeys = sshKeys;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("ssh_keys")
        public List<SshKey> getSshKeys() {
            return sshKeys;
        }
        
        public void setSshKeys(List<SshKey> sshKeys) {
            this.sshKeys = sshKeys;
        }
        
        // For backward compatibility with method reference
        public List<SshKey> sshKeys() {
            return sshKeys;
        }
    }
}
