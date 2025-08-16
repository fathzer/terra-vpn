package com.fathzer.http;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class RequestDecoratorTest {
    private static final URI TEST_URI = URI.create("https://example.com/api");
    private static final Request TEST_REQUEST = new Request(TEST_URI, Method.GET);
    
    @Test
    void testBearerAuth() {
        String token = "test-token-123";
        RequestDecorator decorator = RequestDecorator.bearerAuth(token);
        
        Builder builder = HttpRequest.newBuilder().uri(TEST_URI);
        Builder decorated = decorator.decorate(builder, TEST_REQUEST);
        
        HttpRequest request = decorated.build();
        assertEquals("Bearer " + token, request.headers().firstValue("Authorization").orElse(null));
    }
    
    @Test
    void testBasicAuth() {
        String username = "testUser";
        String password = "testPass";
        String expected = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        
        RequestDecorator decorator = RequestDecorator.basicAuth(username, password);
        
        Builder builder = HttpRequest.newBuilder().uri(TEST_URI);
        Builder decorated = decorator.decorate(builder, TEST_REQUEST);
        
        HttpRequest request = decorated.build();
        assertEquals(expected, request.headers().firstValue("Authorization").orElse(null));
    }
    
    @Test
    void testProducesJson() {
        RequestDecorator decorator = RequestDecorator.producesJson();
        
        Builder builder = HttpRequest.newBuilder().uri(TEST_URI);
        Builder decorated = decorator.decorate(builder, TEST_REQUEST);
        
        HttpRequest request = decorated.build();
        assertEquals("application/json", request.headers().firstValue("Accept").orElse(null));
    }
    
    @Test
    void testConsumesJson() {
        RequestDecorator decorator = RequestDecorator.consumesJson();
        
        Builder builder = HttpRequest.newBuilder().uri(TEST_URI);
        Builder decorated = decorator.decorate(builder, TEST_REQUEST);
        
        HttpRequest request = decorated.build();
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElse(null));
    }
    
    @Test
    void testMultipleDecorators() {
        String token = "test-token";
        RequestDecorator authDecorator = RequestDecorator.bearerAuth(token);
        RequestDecorator jsonDecorator = RequestDecorator.consumesJson();
        
        Builder builder = HttpRequest.newBuilder().uri(TEST_URI);
        builder = authDecorator.decorate(builder, TEST_REQUEST);
        builder = jsonDecorator.decorate(builder, TEST_REQUEST);
        
        HttpRequest request = builder.build();
        assertEquals("Bearer " + token, request.headers().firstValue("Authorization").orElse(null));
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElse(null));
    }
    
    @Test
    @SuppressWarnings("null")
    void testNullParameters() {
        assertThrows(NullPointerException.class, () -> RequestDecorator.bearerAuth(null));
        assertThrows(NullPointerException.class, () -> RequestDecorator.basicAuth(null, "pass"));
        assertThrows(NullPointerException.class, () -> RequestDecorator.basicAuth("user", null));
        
        RequestDecorator decorator = RequestDecorator.bearerAuth("token");
        assertThrows(NullPointerException.class, () -> decorator.decorate(null, TEST_REQUEST));
    }
}
