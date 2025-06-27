package com.fathzer.odvpn;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;

public abstract class AbstractVPSProviderClient implements AutoCloseable {
    public static class ErrorResponseException extends IOException {
        private static final long serialVersionUID = 1L;

		public ErrorResponseException(String message) {
            super(message);
        }
    }

    public static class AuthenticationException extends IOException {
        private static final long serialVersionUID = 1L;

		public AuthenticationException(String message) {
            super(message);
        }
    }

    public static class ServerErrorException extends IOException {
        private static final long serialVersionUID = 1L;

		public ServerErrorException(String message) {
            super(message);
        }
    }

    protected final HttpClient client;
    protected final ObjectMapper objectMapper;
    
    protected AbstractVPSProviderClient() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void close() {
        this.client.close();
    }

    protected HttpRequest.Builder newRequest(URI uri) {
        return HttpRequest.newBuilder().uri(uri);
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
        return new AuthenticationException("Authentication failed");
    }

    protected ErrorResponseException getErrorResponseException(HttpResponse<String> response) throws IOException {
        return new ErrorResponseException("Error " + response.statusCode() + ": " + response.body());
    }

    protected ServerErrorException getServerErrorException(HttpResponse<String> response) throws IOException {
        return new ServerErrorException("Server error " + response.statusCode() + ": " + response.body());
    }
}
