package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;

class VultrClientTest {
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_INSTANCE_ID = "480db6b7-2c94-4f3c-882a-1e2415e589f3";
    
    private VultrClient vultrClient;
    private Function<HttpRequest, HttpResponse<String>> requestHandler;
    
    @BeforeEach
    void setUp() {
        // Create a VultrClient that uses our test request handler
        vultrClient = new VultrClient(TEST_TOKEN) {
            @Override
            protected HttpResponse<String> doRequest(HttpRequest request) throws IOException {
                if (requestHandler == null) {
                    throw new IllegalStateException("No request handler configured");
                }
                return requestHandler.apply(request);
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    private void setupMockResponse(String responseBody) {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        
        requestHandler = request -> {
            assertEquals("https://api.vultr.com/v2/instances/" + TEST_INSTANCE_ID, 
                request.uri().toString());
            assertEquals("Bearer " + TEST_TOKEN, 
                request.headers().firstValue("Authorization").orElse(""));
            return mockResponse;
        };
    }
    
    @Test
    void testGetState_WhenInstanceIsReady_ShouldReturnReadyStatus() throws Exception {
        // Load the test response from the status.json file
        String responseBody = new String(Files.readAllBytes(
            Paths.get("src/test/resources/com/fathzer/odvpn/providers/vultrStatus.json")));
        
        setupMockResponse(responseBody);
        
        // Call the method under test
        VPSState state = vultrClient.getState(TEST_INSTANCE_ID);
        
        // Verify the result
        assertNotNull(state);
        assertEquals(TEST_INSTANCE_ID, state.id());
        assertEquals("65.20.104.140", state.ip());
        assertEquals(Status.READY, state.status());
    }
    
    @Test
    void testGetState_WhenInstanceIsMissing_ShouldReturnStartingStatus() throws Exception {
        // Response with empty instance object
        String responseBody = "{\"instance\": null}";
        
        setupMockResponse(responseBody);
        
        // Call the method under test
        VPSState state = vultrClient.getState(TEST_INSTANCE_ID);
        
        // Verify the result
        assertNotNull(state);
        assertEquals(TEST_INSTANCE_ID, state.id());
        assertNull(state.ip());
        assertEquals(Status.STARTING, state.status());
    }
    
    @Test
    void testGetState_WhenMainIpIsMissing_ShouldReturnStartingStatus() throws Exception {
        // Response with missing main_ip
        String responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + 
            "\",\"power_status\":\"running\",\"server_status\":\"ok\"}}";
        
        setupMockResponse(responseBody);
        
        // Call the method under test
        VPSState state = vultrClient.getState(TEST_INSTANCE_ID);
        
        // Verify the result
        assertNotNull(state);
        assertEquals(TEST_INSTANCE_ID, state.id());
        assertNull(state.ip());
        assertEquals(Status.STARTING, state.status());
    }
    
    @Test
    void testGetState_WhenPowerStatusIsMissing_ShouldReturnIpReadyStatus() throws Exception {
        // Response with missing power_status
        String responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + 
            "\",\"main_ip\":\"65.20.104.140\",\"server_status\":\"ok\"}}";
        
        setupMockResponse(responseBody);
        
        // Call the method under test
        VPSState state = vultrClient.getState(TEST_INSTANCE_ID);
        
        // Verify the result
        assertNotNull(state);
        assertEquals(TEST_INSTANCE_ID, state.id());
        assertEquals("65.20.104.140", state.ip());
        assertEquals(Status.IP_READY, state.status());
    }
    
    @Test
    void testGetState_WhenServerStatusIsMissing_ShouldReturnIpReadyStatus() throws Exception {
        // Response with missing server_status
        String responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + 
            "\",\"main_ip\":\"65.20.104.140\",\"power_status\":\"running\"}}";
        
        setupMockResponse(responseBody);
        
        // Call the method under test
        VPSState state = vultrClient.getState(TEST_INSTANCE_ID);
        
        // Verify the result
        assertNotNull(state);
        assertEquals(TEST_INSTANCE_ID, state.id());
        assertEquals("65.20.104.140", state.ip());
        assertEquals(Status.IP_READY, state.status());
    }
    
    @Test
    void testGetState_WhenMainIpIsEmpty_ShouldReturnStartingStatus() throws Exception {
        // Response with empty main_ip
        String responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + 
            "\",\"main_ip\":\" \",\"power_status\":\"running\",\"server_status\":\"ok\"}}";
        
        setupMockResponse(responseBody);
        
        // Call the method under test
        VPSState state = vultrClient.getState(TEST_INSTANCE_ID);
        
        // Verify the result
        assertNotNull(state);
        assertEquals(TEST_INSTANCE_ID, state.id());
        assertNull(state.ip());
        assertEquals(Status.STARTING, state.status());
    }
}
