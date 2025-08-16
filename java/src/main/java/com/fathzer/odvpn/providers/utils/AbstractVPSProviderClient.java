package com.fathzer.odvpn.providers.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;

public abstract class AbstractVPSProviderClient implements AutoCloseable {
    @FunctionalInterface
    public static interface Authentication {
        public Builder authenticate(Builder builder);
    }

    public static final class TokenAuthentication implements Authentication {
        private final String token;

        public TokenAuthentication(String token) {
            this.token = token;
        }

        @Override
        public Builder authenticate(Builder builder) {
            return builder.header("Authorization", "Bearer " + this.token);
        }
    }

    public static class ResponseException extends IOException {
        private static final long serialVersionUID = 1L;
        private final int statusCode;

        private ResponseException(int statusCode, String message) {
            super(statusCode + ": " + message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return this.statusCode;
        }
    }

    public static class ErrorResponseException extends ResponseException {
        private static final long serialVersionUID = 1L;

		public ErrorResponseException(int statusCode, String message) {
            super(statusCode, message);
        }
    }

    public static class AuthenticationException extends ResponseException {
        private static final long serialVersionUID = 1L;

		public AuthenticationException(int statusCode, String message) {
            super(statusCode, message);
        }
    }

    public static class ServerErrorException extends ResponseException {
        private static final long serialVersionUID = 1L;

		public ServerErrorException(int statusCode, String message) {
            super(statusCode, message);
        }
    }

    private HttpClient client;
    protected final ObjectMapper objectMapper;
    protected final Authentication authentication;
    
    protected AbstractVPSProviderClient(Authentication authentication) {
        this.client = null;
        this.objectMapper = new ObjectMapper();
        this.authentication = authentication;
    }

    protected Builder newRequest(URI uri) {
        return this.authentication.authenticate(HttpRequest.newBuilder().uri(uri));
    }

    /**
     * Gets the HTTP client used by this client.
     * @return the HTTP client
     */
    public HttpClient getHttpClient() {
        if (this.client == null) {
            this.client = HttpClient.newHttpClient();
        }
        return this.client;
    }

    /**
     * Sets the HTTP client to be used by this client.
     * @param client the HTTP client
     */
    public void setHttpClient(HttpClient client) {
        close();
        this.client = client;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.close();
        }
    }

    /**
     * Sends a request to the provider API.
     * @param request the request
     * @return the response
     * @throws IOException if an I/O error occurs. The precise type of exception depends on the implementation of
     *  {@link #getAuthenticationException(HttpResponse)}, {@link #getErrorResponseException(HttpResponse)},
     *  {@link #getServerErrorException(HttpResponse)}.
     */
    public HttpResponse<String> doRequest(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw this.getAuthenticationException(response);
            } else if (response.statusCode() >= 400 && response.statusCode() < 500) {
                throw this.getErrorResponseException(response);
            } else if (response.statusCode() >= 500 && response.statusCode() < 600) {
                throw this.getServerErrorException(response);
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Operation was interrupted");
        }
    }

    protected String getErrorMessage(HttpResponse<String> response) {
        return response.uri()+" - "+response.body();
    }

    /**
     * Builds an authentication exception (called by @link{#doRequest(HttpRequest)} when the response status code is 401 or 403).
     * @param response the response
     * @return the authentication exception
     */
    protected AuthenticationException getAuthenticationException(HttpResponse<String> response) {
        return new AuthenticationException(response.statusCode(), "Authentication failed "+this.getErrorMessage(response));
    }

    /**
     * Builds an error response exception (called by @link{#doRequest(HttpRequest)} when the response status code is between 400 and 499, but not 401 or 403).
     * @param response the response
     * @return the error response exception
     */
    protected ErrorResponseException getErrorResponseException(HttpResponse<String> response) {
        return new ErrorResponseException(response.statusCode(), "Error " + this.getErrorMessage(response));
    }

    /**
     * Builds a server error exception (called by @link{#doRequest(HttpRequest)} when the response status code is between 500 and 599).
     * @param response the response
     * @return the server error exception
     */
    protected ServerErrorException getServerErrorException(HttpResponse<String> response) {
        return new ServerErrorException(response.statusCode(), "Server error " + this.getErrorMessage(response));
    }
}
