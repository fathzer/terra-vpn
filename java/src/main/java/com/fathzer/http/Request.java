package com.fathzer.http;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Represents an HTTP request with its method, URI, headers, and optional body.
 * This class provides a builder-style API for constructing HTTP requests.
 * 
 * <p>Example usage:
 * <pre>{@code
 * Request request = new Request("https://api.example.com")
 *     .path("users")
 *     .param("active", "true")
 *     .header("Accept", "application/json")
 *     .get();
 * }</pre>
 * 
 * @see Method
 * @see UriBuilder
 */
@ParametersAreNonnullByDefault
public class Request {
    /** The HTTP method for this request. */
    private Method method;
    /** The URI builder for constructing the request target. */
    private UriBuilder uriBuilder;
    /** The map of HTTP headers for this request. */
    private Map<String, List<String>> headers;
    /** The request body object, or null if no body. */
    private Object body;

    private Request(UriBuilder uriBuilder, Method method, Map<String, List<String>> headers, @Nullable Object body) {
        Objects.requireNonNull(uriBuilder);
        Objects.requireNonNull(method);
        Objects.requireNonNull(headers);
        if (!method.hasBody() && body != null) {
            throw new IllegalArgumentException("Body is not allowed for method " + method);
        }
        this.method = method;
        this.uriBuilder = uriBuilder;
        this.headers = headers;
        this.body = body;
    }

    /**
     * Creates a GET request with the specified URI and no headers or body.
     *
     * @param baseUri the base URI for the request
     */
    public Request(String baseUri) {
        this(new UriBuilder(baseUri), Method.GET, new HashMap<>(), null);
    }

    /**
     * Sets the request method to POST and sets the request body.
     *
     * @param <T> the type of the body object
     * @param body the request body, may be null
     * @return this request instance for method chaining
     */
    public <T> Request post(@Nullable T body) {
        return this.setBody(Method.POST, body);
    }

    /**
     * Sets the request method to PUT and sets the request body.
     *
     * @param <T> the type of the body object
     * @param body the request body, may be null
     * @return this request instance for method chaining
     */
    public <T> Request put(@Nullable T body) {
        return this.setBody(Method.PUT, body);
    }

    /**
     * Sets the request method to PATCH and sets the request body.
     *
     * @param <T> the type of the body object
     * @param body the request body, may be null
     * @return this request instance for method chaining
     */
    public <T> Request patch(@Nullable T body) {
        return this.setBody(Method.PATCH, body);
    }

    /**
     * Sets the request method to GET and removes any existing body.
     *
     * @return this request instance for method chaining
     */
    public Request get() {
        return this.setBody(Method.GET, null);
    }

    /**
     * Sets the request method to HEAD and removes any existing body.
     *
     * @return this request instance for method chaining
     */
    public Request head() {
        return this.setBody(Method.HEAD, null);
    }

    /**
     * Sets the request method to OPTIONS and removes any existing body.
     *
     * @return this request instance for method chaining
     */
    public Request options() {
        return this.setBody(Method.OPTIONS, null);
    }

    /**
     * Sets the request method to DELETE and removes any existing body.
     *
     * @return this request instance for method chaining
     */
    public Request delete() {
        return this.setBody(Method.DELETE, null);
    }
    
    private <T> Request setBody(Method method, @Nullable T body) {
        if (!method.hasBody() && body != null) {
            throw new IllegalArgumentException("Body is not allowed for method " + method);
        }
        this.body = body;
        this.method = method;
        return this;
    }

    /**
     * Adds a header to the request. Multiple values can be added for the same header name.
     *
     * @param header the header name (case-insensitive)
     * @param value the header value
     * @return this request instance for method chaining
     * @throws NullPointerException if header or value is null
     */
    public Request header(String header, String value) {
        Objects.requireNonNull(header);
        Objects.requireNonNull(value);
        this.headers.computeIfAbsent(header, k -> new LinkedList<>()).add(value);
        return this;
    }

    /**
     * Appends a path segment to the request URI.
     *
     * @param path the path segment to append
     * @return this request instance for method chaining
     * @throws NullPointerException if path is null
     */
    public Request path(String path) {
        this.uriBuilder.pathSegment(path);
        return this;
    }

    /**
     * Adds a query parameter to the request URI.
     *
     * @param name the parameter name
     * @param value the parameter value
     * @return this request instance for method chaining
     * @throws NullPointerException if name or value is null
     */
    public Request param(String name, String value) {
        this.uriBuilder.queryParam(name, value);
        return this;
    }

    /**
     * Returns the HTTP method of this request.
     *
     * @return the HTTP method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns the target URI of this request.
     *
     * @return the target URI
     */
    public URI getUri() {
        return uriBuilder.build();
    }

    /**
     * Returns an unmodifiable map of the HTTP headers for this request.
     * The map keys are header names, and the values are lists of header values.
     *
     * @return the HTTP headers map
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Returns the request body, if any.
     *
     * @return the request body, or null if no body is present
     */
    @Nullable
    public Object getBody() {
        return body;
    }
}
