package com.fathzer.http;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Nullable;

/**
 * A generic HTTP response wrapper that encapsulates the status code, response body, and headers.
 * 
 * @param <T> the type of the response body
 */
@ParametersAreNonnullByDefault
public class Response<T> {
    private final int statusCode;
    private final T body;
    private final Map<String, List<String>> headers;

    /**
     * Constructs a new Response with the specified status code, body, and headers.
     *
     * @param statusCode the HTTP status code
     * @param body the response body, which may be null
     * @param headers the response headers (must not be null)
     * @throws NullPointerException if headers is null
     */
    public Response(int statusCode, @Nullable T body, Map<String, List<String>> headers) {
        Objects.requireNonNull(headers);
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    /**
     * Creates a new Response instance from a standard Java 11+ HttpResponse.
     *
     * @param <T> the type of the response body
     * @param response the HttpResponse to convert from
     * @return a new Response instance containing the same data as the input HttpResponse
     * @throws NullPointerException if response is null
     */
    static <T> Response<T> from(HttpResponse<T> response) {
        return new Response<>(response.statusCode(), response.body(), response.headers().map());
    }

    /**
     * Returns the HTTP status code of this response.
     *
     * @return the HTTP status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the response body, which may be null.
     *
     * @return the response body, or null if there is no body
     */
    @Nullable
    public T body() {
        return body;
    }

    /**
     * Returns an unmodifiable map of the response headers.
     * The keys are the header names in lowercase, and the values are lists of header values.
     *
     * @return the response headers
     */
    public Map<String, List<String>> headers() {
        return headers;
    }
}
