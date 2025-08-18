package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.http.Request;
import com.fathzer.http.Response;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

/**
 * Base class for tests of VPSProviderClient implementations.
 */
public abstract class VPSProviderClientTestBase<T extends BasicVPSProviderClient> {
    protected static final String TEST_TOKEN = "test-token";

    protected T client;
    private record RequestKey(String uri, String method) {}
    private record ResponseData(Response<String> response, Consumer<String> requestBodyCheckConsumer) {}
    private Map<RequestKey, ResponseData> mockResponses = new HashMap<>();

    protected abstract Class<T> getClientClass();

    /**
     * Override this to customize header validation logic per provider.
     * Default: checks for Authorization: Bearer <token>
     */
    protected void validateHeaders(Request request) {
        assertEquals(List.of("Bearer " + TEST_TOKEN), request.getHeaders().get("Authorization"));
    }
    
    @BeforeEach
    void setUp() throws Exception {
        mockResponses.clear();
        client = getClientClass().getConstructor(String.class).newInstance(TEST_TOKEN);
        client.getRestClient().withRequestSender(this::send);
    }
    
    private Response<String> send(HttpClient client, Request request) {
    	String reqUri = request.getUri().toString();
        String reqMethod = request.getMethod().name();
        validateHeaders(request);
        ResponseData respData = mockResponses.get(new RequestKey(reqUri, reqMethod));
        if (respData == null) {
            throw new IllegalStateException("No mock response for URI " + reqUri + " and method " + reqMethod);
        }
        if (respData.requestBodyCheckConsumer != null) {
            final String requestBodyJson = getRequestBodyJson(request);
            assertNotNull(requestBodyJson, "Request body JSON of " + reqMethod + " " + reqUri + " is null");
            respData.requestBodyCheckConsumer.accept(requestBodyJson);
        }
        return respData.response;
    }

    /**
     * Sets up a mock response for a given URI and HTTP method.
     * @param uri The request URI
     * @param method The HTTP method (GET, POST, etc)
     * @param responseBody The mock response body
     * @param requestBodyCheckConsumer A consumer that checks the request body JSON
     */
    protected void setupMockResponse(String uri, String method, String responseBody, Consumer<String> requestBodyCheckConsumer) {
        setupMockResponse(uri, method, 200, responseBody, requestBodyCheckConsumer);
    }

    /**
     * Sets up a mock response for a given URI and HTTP method.
     * @param uri The request URI
     * @param method The HTTP method (GET, POST, etc)
     * @param statusCode The HTTP status code
     * @param responseBody The mock response body
     * @param requestBodyCheckConsumer A consumer that checks the request body JSON
     */
    protected void setupMockResponse(String uri, String method, int statusCode, String responseBody, Consumer<String> requestBodyCheckConsumer) {
		Response<String> mockResponse = new Response<>(statusCode, responseBody, new HashMap<>());
        mockResponses.put(new RequestKey(uri, method.toUpperCase()), new ResponseData(mockResponse, requestBodyCheckConsumer));
    }

    /**
     * Sets up a mock response for a GET request to a given URI.
     * @param uri The request URI
     * @param responseBody The mock response body
     */
    protected void setupMockResponse(String uri, String responseBody) {
        setupMockResponse(uri, "GET", responseBody, null);
    }

    /**
     * Gets body JSON of a Request as a String.
     */
    private static String getRequestBodyJson(Request req) {
        try {
            return new ObjectMapper().writeValueAsString(req.getBody());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
