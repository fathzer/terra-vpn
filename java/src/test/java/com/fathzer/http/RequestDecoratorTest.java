package com.fathzer.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

class RequestDecoratorTest {
    private static final String TEST_URI = "https://example.com/api";
    
    @Test
    void testBearerAuth() {
        String token = "test-token-123";
        Request decorated = new Request(TEST_URI);
        assertSame(decorated, RequestDecorator.bearerAuth(token).decorate(decorated));
        assertEquals("Bearer " + token, decorated.getHeaders().get("Authorization").get(0));
    }
    
    @Test
    void testBasicAuth() {
        String username = "testUser";
        String password = "testPass";
        String expected = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        
        Request decorated = RequestDecorator.basicAuth(username, password).decorate(new Request(TEST_URI));
        assertEquals(expected, decorated.getHeaders().get("Authorization").get(0));
    }
    
    @Test
    void testProducesJson() {
        Request request = new Request(TEST_URI);
        RequestDecorator decorator = RequestDecorator.acceptJson();
        Request decorated = decorator.decorate(request);
        assertEquals("application/json", decorated.getHeaders().get("Accept").get(0));
    }
    
    @Test
    void testSendJson() {
        RequestDecorator decorator = RequestDecorator.sendJson();
        
        Request decorated = decorator.decorate(new Request(TEST_URI));
        assertNull(decorated.getHeaders().get("Content-Type"));

        Request request = new Request(TEST_URI).post("{\"body\":{}}");
        decorator.decorate(request);
        assertEquals(List.of("application/json"), request.getHeaders().get("Content-Type"));
    }
    
    @Test
    void testMultipleDecorators() {
        String token = "test-token";
        RequestDecorator authDecorator = RequestDecorator.bearerAuth(token);
        RequestDecorator jsonDecorator = RequestDecorator.sendJson();
        
        Request request = new Request(TEST_URI).post("{\"body\":{}}");
        request = authDecorator.decorate(request);
        request = jsonDecorator.decorate(request);
        
        assertEquals(List.of("Bearer " + token), request.getHeaders().get("Authorization"));
        assertEquals(List.of("application/json"), request.getHeaders().get("Content-Type"));
    }
    
    @Test
    @SuppressWarnings("null")
    void testNullParameters() {
        assertThrows(NullPointerException.class, () -> RequestDecorator.bearerAuth(null));
        assertThrows(NullPointerException.class, () -> RequestDecorator.basicAuth(null, "pass"));
        assertThrows(NullPointerException.class, () -> RequestDecorator.basicAuth("user", null));
        
        final RequestDecorator decorator = RequestDecorator.bearerAuth("token");
        assertThrows(NullPointerException.class, () -> decorator.decorate(null));
    }
}
