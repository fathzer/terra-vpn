package com.fathzer.http;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@SuppressWarnings("null")
class RequestTest {
    private static final URI TEST_URI = URI.create("https://example.com/api");
    private static record TestBody(String str, int code) {}
    private static final TestBody TEST_BODY = new TestBody("test body", 42);
    private static final Map<String, List<String>> TEST_HEADERS = 
        Map.of("Content-Type", List.of("application/json"));
    
    @Test
    void testConstructorWithUriOnly() {
        Request request = new Request(TEST_URI);
        
        assertEquals(Method.GET, request.getMethod());
        assertEquals(TEST_URI, request.getUri());
        assertTrue(request.getHeaders().isEmpty());
        assertNull(request.getBody());
        assertNull(request.getBodyAsString());
    }
    
    @Test
    void testConstructorWithMethodAndUri() {
        Request request = new Request(TEST_URI, Method.POST);
        
        assertEquals(Method.POST, request.getMethod());
        assertEquals(TEST_URI, request.getUri());
        assertTrue(request.getHeaders().isEmpty());
        assertNull(request.getBody());
        assertNull(request.getBodyAsString());
    }
    
    @Test
    void testConstructorWithMethodUriAndHeaders() {
        Request request = new Request(TEST_URI, Method.PUT, TEST_HEADERS, null, null);
        
        assertEquals(Method.PUT, request.getMethod());
        assertEquals(TEST_URI, request.getUri());
        assertEquals(TEST_HEADERS, request.getHeaders());
        assertNull(request.getBody());
        assertNull(request.getBodyAsString());
    }
    
    @Test
    void testConstructorWithMethodUriAndBody() {
        Request request = new Request(TEST_URI, Method.POST, TEST_HEADERS, TEST_BODY, TEST_BODY.toString());
        
        assertEquals(Method.POST, request.getMethod());
        assertEquals(TEST_URI, request.getUri());
        assertEquals(TEST_HEADERS, request.getHeaders());
        assertEquals(TEST_BODY, request.getBody());
        assertEquals(TEST_BODY.toString(), request.getBodyAsString());
    }
    
    @Test
    void testSetBody() {
        Request request = new Request(TEST_URI, Method.POST);
        request.setBody(TEST_BODY, Object::toString);
        
        assertEquals(TEST_BODY, request.getBody());
        assertEquals(TEST_BODY.toString(), request.getBodyAsString());

        assertThrows(NullPointerException.class, () -> request.setBody(null, Object::toString));
        assertThrows(NullPointerException.class, () -> request.setBody(TEST_BODY, null));
    }
    
    @Test
    void testSetHeaders() {
        Request request = new Request(TEST_URI);
        request.setHeaders(TEST_HEADERS);
        
        assertEquals(TEST_HEADERS, request.getHeaders());
        assertThrows(NullPointerException.class, () -> request.setHeaders(null));
    }
    
    @ParameterizedTest
    @EnumSource(value = Method.class, names = {"GET", "HEAD", "DELETE", "OPTIONS"})
    void testBodyNotAllowedForMethod(Method method) {
        assertThrows(IllegalArgumentException.class, 
            () -> new Request(TEST_URI, method, TEST_HEADERS, TEST_BODY, TEST_BODY.toString()),
            "Should not allow body for method: " + method);
    }
    
    @Test
    void testNullInConstructors() {
        // Null method should throw NullPointerException
        assertThrows(NullPointerException.class, () -> new Request(TEST_URI, null));
        // Null uri should throw NullPointerException
        assertThrows(NullPointerException.class, () -> new Request(null));
        // Null headers should throw NullPointerException
        assertThrows(NullPointerException.class, () -> new Request(TEST_URI, Method.GET, null, null, null));

        // Test that body and bodyAsString must be both null or both non-null
        assertThrows(IllegalArgumentException.class, 
            () -> new Request(TEST_URI, Method.POST, TEST_HEADERS, TEST_BODY, null));
        final String asString = TEST_BODY.toString();
        assertThrows(IllegalArgumentException.class, 
            () -> new Request(TEST_URI, Method.POST, TEST_HEADERS, null, asString));
    }
}
