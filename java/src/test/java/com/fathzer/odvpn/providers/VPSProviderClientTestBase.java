package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

/**
 * Base class for tests of VPSProviderClient implementations.
 */
public abstract class VPSProviderClientTestBase<T extends BasicVPSProviderClient> {
    protected static final String TEST_TOKEN = "test-token";

    protected T client;
    private record RequestKey(String uri, String method) {}
    private record ResponseData(HttpResponse<String> response, Consumer<String> requestBodyCheckConsumer) {}
    private Map<RequestKey, ResponseData> mockResponses = new HashMap<>();

    protected abstract Class<T> getClientClass();

    /**
     * Override this to customize header validation logic per provider.
     * Default: checks for Authorization: Bearer <token>
     */
    protected void validateHeaders(HttpRequest request) {
        assertEquals("Bearer " + TEST_TOKEN, request.headers().firstValue("Authorization").orElse(""));
    }
    @SuppressWarnings({"unchecked"})
    @BeforeEach
    void setUp() {
        mockResponses.clear();
        // Create a mock HttpClient
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        try {
            client = getClientClass().getConstructor(String.class).newInstance(TEST_TOKEN);
            client.setHttpClient(mockHttpClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate client", e);
        }
        // Set up the mock to return the appropriate HttpResponse for each request
        try {
            Mockito.when(mockHttpClient.send(Mockito.any(HttpRequest.class), Mockito.any(HttpResponse.BodyHandler.class)))
                    .thenAnswer((Answer<HttpResponse<String>>) invocation -> {
                        HttpRequest request = invocation.getArgument(0);
                        String reqUri = request.uri().toString();
                        String reqMethod = request.method().toUpperCase();
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
                        return respData.response();
                    });
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        @SuppressWarnings("unchecked")
		HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(statusCode);
        when(mockResponse.body()).thenReturn(responseBody);
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
     * Gets body JSON of a HttpRequest as a String.
     */
    private static String getRequestBodyJson(HttpRequest req) {
        return req.bodyPublisher().map(bp -> {
            StringBuilder sb = new StringBuilder();
            bp.subscribe(new Flow.Subscriber<ByteBuffer>() {
                @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
                @Override public void onNext(ByteBuffer bb) {
                    byte[] bytes = new byte[bb.remaining()];
                    bb.get(bytes);
                    sb.append(new String(bytes, StandardCharsets.UTF_8));
                }
                @Override public void onError(Throwable throwable) {
                    // Does nothing
                }
                @Override public void onComplete() {
                    // Does nothing
                }
            });
            return sb.toString();
        }).orElse(null);
    }
}
