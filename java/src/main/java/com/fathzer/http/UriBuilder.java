package com.fathzer.http;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A lightweight URI builder that handles proper URL encoding of path segments and query parameters.
 * This class provides a fluent API for constructing URIs with automatic encoding of special characters.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Automatic URL encoding of path segments and query parameters</li>
 *   <li>Support for multiple values per query parameter</li>
 *   <li>Support for empty path segments (allowed by HTTP specification)</li>
 *   <li>Fluent API for easy chaining</li>
 *   <li>No external dependencies</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * URI uri = CustomUriBuilder.create()
 *     .scheme("https")
 *     .host("api.example.com")
 *     .pathSegment("users")
 *     .pathSegment("123")
 *     .queryParam("format", "json")
 *     .queryParam("tags", "java")
 *     .queryParam("tags", "spring")
 *     .build();
 * }</pre>
 */
@ParametersAreNonnullByDefault
final class UriBuilder {
    private final String scheme;
    private final String host;
    private final int port;
    private final List<String> pathSegments;
    private final Map<String, List<String>> queryParams;
    
    /**
     * Creates a new URI builder from an existing URI string.
     * The existing path segments and query parameters are preserved.
     * 
     * @param baseUri the base URI to start from
     * @return a new CustomUriBuilder instance initialized with the base URI
     * @throws IllegalArgumentException if the base URI is invalid
     */
    UriBuilder(String baseUri) {
        try {
            URI uri = new URI(baseUri);
            this.scheme = uri.getScheme();
            this.host = uri.getHost();
            this.port = uri.getPort();
            this.pathSegments = new LinkedList<>();
            this.queryParams = new HashMap<>();
            
            // Add existing path segments (preserving empty segments)
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                // Split while preserving empty segments
                String[] segments = path.split("/", -1);
                // Skip the first empty segment if path starts with "/"
                int start = path.startsWith("/") ? 1 : 0;
                for (int i = start; i < segments.length; i++) {
                    this.pathSegments.add(segments[i]);
                }
            }
            
            // Parse existing query parameters
            String query = uri.getQuery();
            if (query != null && !query.isEmpty()) {
                String[] params = query.split("&");
                for (String param : params) {
                    int eqIndex = param.indexOf('=');
                    if (eqIndex > 0) {
                        String name = urlDecode(param.substring(0, eqIndex));
                        String value = urlDecode(param.substring(eqIndex + 1));
                        this.queryParam(name, value);
                    } else {
                        // Parameter without value
                        this.queryParam(urlDecode(param), "");
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI: " + baseUri, e);
        }
    }
    
    /**
     * Appends a single path segment. Empty segments are allowed.
     * The segment will be properly URL-encoded when building the URI.
     * 
     * @param segment the path segment to append ("" for an empty segment)
     * @return this builder for method chaining
     */
    UriBuilder pathSegment(String segment) {
        Objects.requireNonNull(segment);
        pathSegments.add(segment); // Allow empty segments
        return this;
    }
    
    /**
     * Adds a query parameter. If a parameter with the same name already exists,
     * this will add an additional value (resulting in multiple parameters with the same name).
     * 
     * @param name the parameter name
     * @param value the parameter value ("" for an empty value)
     * @return this builder for method chaining
     */
    UriBuilder queryParam(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        queryParams.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }
    
    /**
     * Builds the final URI with proper encoding of all components.
     * 
     * @return the constructed URI
     * @throws RuntimeException if URI construction fails
     */
    URI build() {
        return URI.create(toString());
    }
    
    /**
     * URL-encodes a string using UTF-8 encoding.
     * 
     * @param value the string to encode
     * @return the URL-encoded string
     */
    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    
    /**
     * URL-decodes a string using UTF-8 encoding.
     * 
     * @param value the string to decode
     * @return the URL-decoded string
     */
    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
    
    /**
     * Returns the string representation of the built URI.
     * 
     * @return the URI as a string
     */
    @Override
    public String toString() {
        StringBuilder uriBuilder = new StringBuilder();
        // Build scheme and authority
        uriBuilder.append(scheme).append("://").append(host);
        if (port != -1) {
            uriBuilder.append(":").append(port);
        }
        // Build path with encoded segments
        for (String segment : pathSegments) {
            uriBuilder.append("/").append(urlEncode(segment));
        }
        // Build query with encoded parameters
        if (!queryParams.isEmpty()) {
            uriBuilder.append("?");
            boolean first = true;
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                String encodedName = urlEncode(entry.getKey());
                for (String value : entry.getValue()) {
                    if (!first) {
                        uriBuilder.append("&");
                    }
                    uriBuilder.append(encodedName).append("=").append(urlEncode(value));
                    first = false;
                }
            }
        }
        return uriBuilder.toString();
    }
}