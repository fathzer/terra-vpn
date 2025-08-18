package com.fathzer.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ResponseTest {

    @Test
    void testConstructorAndGetters() {
        // Given
        int expectedStatusCode = 200;
        String expectedBody = "test body";
        Map<String, List<String>> headers = Map.of("Content-Type", List.of("application/json"));
        
        // When
        Response<String> response = new Response<>(expectedStatusCode, expectedBody, headers);
        
        // Then
        assertEquals(expectedStatusCode, response.statusCode());
        assertEquals(expectedBody, response.body());
        assertEquals(headers, response.headers());
        assertSame(headers, response.headers()); // Should return the exact same map instance
    }
    
    @SuppressWarnings("null")
    @Test
    void testConstructorWithNullHeaders() {
        // Given
        int statusCode = 200;
        String body = "test body";
        
        // When/Then
        assertThrows(NullPointerException.class, () -> new Response<>(statusCode, body, null));
    }
    
    @Nested
    class FromHttpResponse {
        private static String expectedBody = "test body";
        private static Map<String, List<String>> headersMap = Map.of(
            "Content-Type", List.of("application/json"),
            "X-Custom-Header", List.of("value1", "value2")
        );
        private static HttpHeaders headers = HttpHeaders.of(headersMap, (s1, s2) -> true);
        private HttpResponse<String> httpResponse;

        @SuppressWarnings("unchecked")
        @BeforeEach
        void setUp() {
            httpResponse = mock(HttpResponse.class);
            when(httpResponse.headers()).thenReturn(headers);
        }

        @Test
        void testFromHttpResponse() {
            when(httpResponse.statusCode()).thenReturn(201);
            when(httpResponse.body()).thenReturn(expectedBody);
            
            // When
            Response<String> response = Response.from(httpResponse);
            
            // Then
            assertEquals(201, response.statusCode());
            assertEquals(expectedBody, response.body());
            assertEquals(headersMap, response.headers());
            
            // Verify header access
            assertEquals(List.of("application/json"), response.headers().get("content-type"));
            assertEquals(List.of("value1", "value2"), response.headers().get("x-custom-header"));
            
        }
        
        @Test
        void testFromHttpResponseWithNullBody() {
            when(httpResponse.statusCode()).thenReturn(204);
            when(httpResponse.body()).thenReturn(null);
            when(httpResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (s1, s2) -> true));
            
            // When
            Response<String> response = Response.from(httpResponse);
            
            // Then
            assertEquals(204, response.statusCode());
            assertNull(response.body());
            assertTrue(response.headers().isEmpty());
        }

        @SuppressWarnings("null")
        @Test
        void testFromNullHttpResponse() {
            assertThrows(NullPointerException.class, () -> Response.from(null));
        }
    }
}
