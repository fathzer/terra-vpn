package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;

class VultrClientTest {
    private static final String API_URL = "https://api.vultr.com/v2/";
    private static final String INSTANCES_PATH = API_URL + "instances/";

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
    private void setupMockResponse(String uri, String responseBody) {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        
        requestHandler = request -> {
            assertEquals(uri, request.uri().toString());
            assertEquals("Bearer " + TEST_TOKEN, request.headers().firstValue("Authorization").orElse(""));
            return mockResponse;
        };
    }
    
    @Test
    void testGetState() throws Exception {
        final String uri = INSTANCES_PATH + TEST_INSTANCE_ID;
        // Ready
        String responseBody = "{\"instance\": {\"main_ip\": \"65.20.104.140\",\"power_status\": \"running\","+
            "\"server_status\": \"ok\",\"more\": \"just to check it will work if Vultr returns more data\"}}";
        setupMockResponse(uri, responseBody);
        assertEquals(new VPSState(TEST_INSTANCE_ID, "65.20.104.140", Status.READY), vultrClient.getState(TEST_INSTANCE_ID));
        // Response with empty instance object
        setupMockResponse(uri, "{\"instance\": null}");
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), vultrClient.getState(TEST_INSTANCE_ID));
        // Response with missing main_ip
        setupMockResponse(uri, "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + "\",\"power_status\":\"running\",\"server_status\":\"ok\"}}");
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), vultrClient.getState(TEST_INSTANCE_ID));
        // Response with missing power_status
        responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + "\",\"main_ip\":\"65.20.104.140\",\"server_status\":\"ok\"}}";
        setupMockResponse(uri, responseBody);
        assertEquals(new VPSState(TEST_INSTANCE_ID, "65.20.104.140", Status.IP_READY), vultrClient.getState(TEST_INSTANCE_ID));
        // Response with missing server_status
        responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + "\",\"main_ip\":\"65.20.104.140\",\"power_status\":\"running\"}}";
        setupMockResponse(uri, responseBody);
        assertEquals(new VPSState(TEST_INSTANCE_ID, "65.20.104.140", Status.IP_READY), vultrClient.getState(TEST_INSTANCE_ID));
        // Response with empty main_ip
        responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + "\",\"main_ip\":\" \",\"power_status\":\"running\",\"server_status\":\"ok\"}}";
        setupMockResponse(uri, responseBody);
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), vultrClient.getState(TEST_INSTANCE_ID));
    }
}
