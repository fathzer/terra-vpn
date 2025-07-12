package com.fathzer.odvpn;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;

public abstract class AbstractVPSProviderClient implements AutoCloseable {
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

    protected final HttpClient client;
    protected final ObjectMapper objectMapper;
    protected final Authentication authentication;
    
    protected AbstractVPSProviderClient(Authentication authentication) {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.authentication = authentication;
    }

    protected Builder newRequest(URI uri) {
        return this.authentication.authenticate(HttpRequest.newBuilder().uri(uri));
    }

    @Override
    public void close() {
        this.client.close();
    }

    protected HttpResponse<String> doRequest(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
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

    protected AuthenticationException getAuthenticationException(HttpResponse<String> response) throws IOException {
        return new AuthenticationException(response.statusCode(), "Authentication failed");
    }

    protected ErrorResponseException getErrorResponseException(HttpResponse<String> response) throws IOException {
        return new ErrorResponseException(response.statusCode(), "Error " + response.statusCode() + ": " + response.body());
    }

    protected ServerErrorException getServerErrorException(HttpResponse<String> response) throws IOException {
        return new ServerErrorException(response.statusCode(), "Server error " + response.statusCode() + ": " + response.body());
    }
}
