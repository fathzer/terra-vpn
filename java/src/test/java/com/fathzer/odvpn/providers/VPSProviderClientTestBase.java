package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.utils.IOLambdas.IOFunction;

public abstract class VPSProviderClientTestBase {
    private static final String TEST_TOKEN = "test-token";

    protected BasicVPSProviderClient client;
    protected IOFunction<HttpRequest, HttpResponse<String>> requestHandler;

    protected abstract Class<? extends BasicVPSProviderClient> getClientClass();

    @BeforeEach
    void setUp() {
        // Create a mock of the client to test
        client = Mockito.mock(getClientClass(), Mockito.withSettings()
                .useConstructor(TEST_TOKEN)
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        // Mock the doRequest method to delegate to requestHandler
        Answer<HttpResponse<String>> answer = (Answer<HttpResponse<String>>) invocation -> {
            HttpRequest request = invocation.getArgument(0);
            if (requestHandler == null) {
                throw new IllegalStateException("No request handler configured");
            }
            return requestHandler.apply(request);
        };
        try {
            Mockito.doAnswer(answer).when(client).doRequest(Mockito.any(HttpRequest.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

        @SuppressWarnings("unchecked")
    protected void setupMockResponse(String uri, String responseBody) {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        
        requestHandler = request -> {
            assertEquals(uri, request.uri().toString());
            assertEquals("Bearer " + TEST_TOKEN, request.headers().firstValue("Authorization").orElse(""));
            return mockResponse;
        };
    }
}
