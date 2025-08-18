package com.fathzer.http;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Base exception for HTTP request errors
 */
@ParametersAreNonnullByDefault
public abstract class RequestException extends IOException {
    private static final long serialVersionUID = 1L;

    /**
     * Exception for client errors (4xx)
     */
    public static class ClientErrorException extends RequestException {
        private static final long serialVersionUID = 1L;
        public ClientErrorException(Request request, Response<String> response) {
            super(request, response);
        }
    }

    /**
     * Exception for authentication errors (401, 403)
     */
    @SuppressWarnings("java:S110")
    public static class AuthenticationException extends ClientErrorException {
        private static final long serialVersionUID = 1L;
        public AuthenticationException(Request request, Response<String> response) {
            super(request, response);
        }
    }
    
    /**
     * Exception for server errors (5xx)
     */
    public static class ServerErrorException extends RequestException {
        private static final long serialVersionUID = 1L;
        public ServerErrorException(Request request, Response<String> response) {
            super(request, response);
        }
    }
    
    private final transient Request request;
    private final transient Response<String> response;

    protected RequestException(Request request, Response<String> response) {
        super(buildMessage(request, response));
        this.request = request;
        this.response = response;
    }

    public int getStatusCode() {
        return this.response.statusCode();
    }

    public Request getRequest() {
        return this.request;
    }
    public Response<String> getResponse() {
        return this.response;
    }

    protected String getErrorMessage() {
        return request.getMethod() + "-" + request.getUri() + " - " + response.body();
    }
    
    private static String buildMessage(Request request, Response<String> response) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(response);
        final StringBuilder sb = new StringBuilder();
        sb.append(" [").append(request.getMethod()).append(':').append(request.getUri());
        if (request.getBody() != null) {
            sb.append(" - body: ").append(request.getBody());
        }
        sb.append(']');

        sb.append(" => [").append(response.statusCode());
        if (response.body() != null && !response.body().trim().isEmpty()) {
            sb.append("\nResponse body: ").append(response.body());
        }
        sb.append(']');
        
        return sb.toString();
    }
}