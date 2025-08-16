package com.fathzer.http;

import java.net.http.HttpRequest.Builder;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Functional interface for decorating requests before sending
 */
@FunctionalInterface
@ParametersAreNonnullByDefault
interface RequestDecorator {
    /**
     * Creates a decorator for Bearer authentication
     */
    static RequestDecorator bearerAuth(String token) {
        Objects.requireNonNull(token);
        return (builder, request) -> builder.header("Authorization", "Bearer " + token);
    }
    
    /**
     * Creates a decorator for Basic authentication
     */
    static RequestDecorator basicAuth(String username, String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        final String credentials = java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        return (builder, request) -> builder.header("Authorization", "Basic " + credentials);
    }

    /**
     * Creates a decorator for JSON production
     * @return
     */
    static RequestDecorator producesJson() {
        return (builder, request) -> builder.header("Accept", "application/json");
    }

    /**
     * Creates a decorator for JSON consumption
     * @return
     */
    static RequestDecorator consumesJson() {
        return (builder, request) -> builder.header("Content-Type", "application/json");
    }

    /**
     * Decorates the request builder with the given request
     * @param requestBuilder the request builder to decorate
     * @param request the request to decorate
     * @return the decorated request builder
     */
    Builder decorate(Builder requestBuilder, Request request);
}