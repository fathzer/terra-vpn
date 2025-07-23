package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;


public abstract class VPSProviderClientTestBase {
    private static final String TEST_TOKEN = "test-token";

    protected BasicVPSProviderClient client;
    protected record RequestKey(String uri, String method) {}
    protected Map<RequestKey, HttpResponse<String>> mockResponses = new HashMap<>();

    protected abstract Class<? extends BasicVPSProviderClient> getClientClass();

    @BeforeEach
    void setUp() {
        // Clear mock responses to avoid test pollution
        mockResponses.clear();
        // Create a mock of the client to test
        client = Mockito.mock(getClientClass(), Mockito.withSettings()
                .useConstructor(TEST_TOKEN)
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        // Mock the doRequest method to delegate to mockResponses
        Answer<HttpResponse<String>> answer = (Answer<HttpResponse<String>>) invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String reqUri = request.uri().toString();
            String reqMethod = request.method().toUpperCase();
            assertEquals("Bearer " + TEST_TOKEN, request.headers().firstValue("Authorization").orElse(""));
            HttpResponse<String> resp = mockResponses.get(new RequestKey(reqUri, reqMethod));
            if (resp == null) {
                throw new IllegalStateException("No mock response for URI " + reqUri + " and method " + reqMethod);
            }
            return resp;
        };
        try {
            Mockito.doAnswer(answer).when(client).doRequest(Mockito.any(HttpRequest.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Sets up a mock response for a given URI and HTTP method.
     * @param uri The request URI
     * @param method The HTTP method (GET, POST, etc)
     * @param responseBody The mock response body
     */
    protected void setupMockResponse(String uri, String method, String responseBody) {
        @SuppressWarnings("unchecked")
		HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        mockResponses.put(new RequestKey(uri, method.toUpperCase()), mockResponse);
    }

    /**
     * Backward-compatible version for GET requests only.
     */
    protected void setupMockResponse(String uri, String responseBody) {
        setupMockResponse(uri, "GET", responseBody);
    }
}
