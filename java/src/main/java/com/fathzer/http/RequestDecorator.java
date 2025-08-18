package com.fathzer.http;

import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Functional interface for decorating requests before sending
 */
@FunctionalInterface
@ParametersAreNonnullByDefault
public interface RequestDecorator {
    /**
     * Creates a decorator for Bearer authentication
     */
    static RequestDecorator bearerAuth(String token) {
        Objects.requireNonNull(token);
        return request -> request.header("Authorization", "Bearer " + token);
    }
    
    /**
     * Creates a decorator for Basic authentication
     */
    static RequestDecorator basicAuth(String username, String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        final String credentials = java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        return request -> request.header("Authorization", "Basic " + credentials);
    }

    /**
     * Creates a decorator for JSON production
     * @return
     */
    static RequestDecorator acceptJson() {
        return request -> request.header("Accept", "application/json");
    }

    /**
     * Creates a decorator for JSON consumption
     * @return
     */
    static RequestDecorator sendJson() {
        return request -> {
            if (request.getBody() != null) {
                request.header("Content-Type", "application/json");
            }
            return request;
        };
    }

    /**
     * Decorates the request builder with the given request
     * @param request the request to decorate
     * @return the decorated request
     */
    Request decorate(Request request);
}