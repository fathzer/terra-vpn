package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
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
public abstract class VPSProviderClientTestBase {
    private static final String TEST_TOKEN = "test-token";

    protected BasicVPSProviderClient client;
    private record RequestKey(String uri, String method) {}
    private record ResponseData(HttpResponse<String> response, Consumer<String> requestBodyCheckConsumer) {}
    private Map<RequestKey, ResponseData> mockResponses = new HashMap<>();

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
    protected void setupMockResponse(String uri, String method, String responseBody, Consumer<String> requestBodyCheckConsumer) {
        @SuppressWarnings("unchecked")
		HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
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
                @Override public void onError(Throwable throwable) {}
                @Override public void onComplete() {}
            });
            return sb.toString();
        }).orElse(null);
    }
}
