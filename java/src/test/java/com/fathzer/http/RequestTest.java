package com.fathzer.http;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

@SuppressWarnings("null")
class RequestTest {
    private static final String TEST_URI = "https://example.com/api";
    private static record TestBody(String str, int code) {}
    private static final TestBody TEST_BODY = new TestBody("test body", 42);

    @Test
    void testConstructor() {
        Request request = new Request(TEST_URI);
        
        assertEquals(Method.GET, request.getMethod());
        assertEquals(TEST_URI, request.getUri().toString());
        assertTrue(request.getHeaders().isEmpty());
        assertNull(request.getBody());
    }
    
    @Test
    void testSetMethodAndBody() {
        Request request = new Request(TEST_URI);

        request.post(TEST_BODY);
        assertEquals(Method.POST, request.getMethod());
        assertEquals(TEST_BODY, request.getBody());

        request.delete();
        assertEquals(Method.DELETE, request.getMethod());
        assertNull(request.getBody());

        request.put(TEST_BODY);
        assertEquals(TEST_BODY, request.getBody());
        assertEquals(Method.PUT, request.getMethod());


        request.options();
        assertEquals(Method.OPTIONS, request.getMethod());
        assertNull(request.getBody());

        request.patch(TEST_BODY);
        assertEquals(TEST_BODY, request.getBody());
        assertEquals(Method.PATCH, request.getMethod());

        request.head();
        assertEquals(Method.HEAD, request.getMethod());
        assertNull(request.getBody());

        request.post(TEST_BODY);
        request.get();
        assertEquals(Method.GET, request.getMethod());
        assertNull(request.getBody());

        // Assert that the body can be set to null without changing the method
        request.post(TEST_BODY);
        request.post(null);
        assertEquals(Method.POST, request.getMethod());
        assertNull(request.getBody());
        
    }
    
    @Test
    void testNull() {
        // Null uri should throw NullPointerException
        assertThrows(NullPointerException.class, () -> new Request(null));
        Request req = new Request(TEST_URI);

        // Null in header should throw NullPointerException
        assertThrows(NullPointerException.class, () -> req.header(null,"value"));
        assertThrows(NullPointerException.class, () -> req.header("key",null));

        // Null in path should throw NullPointerException
        assertThrows(NullPointerException.class, () -> req.path(null));

        // Null in param name or value should throw NullPointerException
        assertThrows(NullPointerException.class, () -> req.param(null, "value"));
        assertThrows(NullPointerException.class, () -> req.param("key", null));
        
        // Null in body should not throw NullPointerException
        assertDoesNotThrow(() -> req.post(null));
        assertDoesNotThrow(() -> req.put(null));
        assertDoesNotThrow(() -> req.patch(null));
    }
}
