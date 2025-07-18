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

class HetznerClientTest {
    private static final String API_URL = "https://api.hetzner.cloud/v1/";
    private static final String SERVERS_PATH = API_URL + "servers/";
    
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_INSTANCE_ID = "103147789";
    private static final String IP = "65.20.104.140";

    private static final String RESPONSE_BODY_FORMAT = """
    		{"server": {"id": "103147789","status":"%s","public_net": {"ipv4":%s}}}
    """;
    private static final String IP_V4_JSON = "{\"id\":12,\"ip\": \""+IP+"\"}}";

    private HetznerClient hetznerClient;
    private Function<HttpRequest, HttpResponse<String>> requestHandler;
    
    @BeforeEach
    void setUp() {
        // Create a VultrClient that uses our test request handler
        hetznerClient = new HetznerClient(TEST_TOKEN) {
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
    private void setupServersMockResponse(String uri, String responseBody) {
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
        final String uri = SERVERS_PATH + TEST_INSTANCE_ID;
        // STARTING when no server object
        setupServersMockResponse(uri, "{\"server\": null}");
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), hetznerClient.getState(TEST_INSTANCE_ID));
        // STARTING when null ipv4
        setupServersMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "starting", null));
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), hetznerClient.getState(TEST_INSTANCE_ID));
        // IP_READY when ipv4 is provided
        setupServersMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "starting", IP_V4_JSON));
        assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.IP_READY), hetznerClient.getState(TEST_INSTANCE_ID));
        // READY
        setupServersMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "running", IP_V4_JSON));
        assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.READY), hetznerClient.getState(TEST_INSTANCE_ID));
    }
}
