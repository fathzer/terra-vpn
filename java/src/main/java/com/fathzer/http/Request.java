package com.fathzer.http;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Request {
    private Method method;
    private URI uri;
    private Map<String, List<String>> headers;
    private Object body;
    private String bodyAsString;

    /**
     * Creates a request with the specified method, URI, and body, but no additional headers.
     * @param uri the target URI for the request
     * @param method the HTTP method (GET, POST, etc.)
     * @param headers a map of HTTP headers where keys are header names and values are lists of header values
     * @param body the request body, or null if no body should be sent
     */
    public Request(URI uri, Method method, Map<String, List<String>> headers, @Nullable Object body, @Nullable String bodyAsString) {
        Objects.requireNonNull(method);
        Objects.requireNonNull(uri);
        Objects.requireNonNull(headers);
        if (!method.hasBody() && body != null) {
            throw new IllegalArgumentException("Body is not allowed for method " + method);
        }
        if (bodyAsString != null && body == null) {
            throw new IllegalArgumentException("Serializer is not allowed for null body");
        }
        if (bodyAsString == null && body != null) {
            throw new IllegalArgumentException("Serializer is required for non-null body");
        }
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
        this.bodyAsString = bodyAsString;
    }

    /**
     * Creates a GET request with the specified URI and no headers or body.
     *
     * @param uri the target URI for the request
     */
    public Request(URI uri) {
        this(uri, Method.GET, Map.of(), null, null);
    }
    /**
     * Creates a request with the specified method and URI, and no headers or body.
     *
     * @param method the HTTP method to use
     * @param uri the target URI for the request
     */
    public Request(URI uri, Method method) {
        this(uri, method, Map.of(), null, null);
    }

    public <T> void setBody(T body, Function<T, String> serializer) {
        this.body = body;
        this.bodyAsString = serializer.apply(body);
    }

    public void setHeaders(Map<String, List<String>> headers) {
        Objects.requireNonNull(headers);
        this.headers = headers;
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
        return uri;
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

    /**
     * Returns the request body as a string, if any.
     *
     * @return the request body as a string, or null if no body is present
     */
    @Nullable
    public String getBodyAsString() {
        return bodyAsString;
    }
}
