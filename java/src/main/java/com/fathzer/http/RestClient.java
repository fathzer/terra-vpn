package com.fathzer.http;

import com.fathzer.http.RequestException.AuthenticationException;
import com.fathzer.http.RequestException.ClientErrorException;
import com.fathzer.http.RequestException.ServerErrorException;

import io.micrometer.common.lang.Nullable;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A REST client that simplifies making HTTP requests and handling responses.
 * This class provides a higher-level API over Java's HttpClient,
 * with built-in request/response serialization/deserialization and error handling.
 * 
 * <p>Subclasses must implement serialization logic for request/response bodies.
 * The client supports request decoration through {@link RequestDecorator} instances
 * that can modify requests before they are sent.
 */
@ParametersAreNonnullByDefault
public class RestClient implements AutoCloseable {
    @FunctionalInterface
    public interface RequestSender {
        HttpResponse<String> send(HttpClient httpClient, Request request) throws IOException;
    }

    /**
     * The underlying HTTP client used to execute requests.
     */
    private final HttpClient httpClient;
    
    /**
     * List of decorators that can modify requests before they are sent.
     */
    private final List<RequestDecorator> requestDecorators;

    private RequestSender requestSender = this::send;
    
    /**
     * Creates a new RestClient with default settings.
     * Uses a default HttpClient with a 30-second connection timeout.
     */
    public RestClient() {
        this(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build());
    }
    
    /**
     * Creates a new RestClient with the specified HttpClient.
     * @param httpClient the HttpClient to use for making requests
     * @throws NullPointerException if httpClient is null
     */
    public RestClient(HttpClient httpClient) {
        Objects.requireNonNull(httpClient);
        this.httpClient = httpClient;
        this.requestDecorators = new ArrayList<>();
    }
    
    /**
     * Adds a request decorator that will be applied to all requests.
     * Decorators are applied in the order they are added.
     *
     * @param decorator the decorator to add
     * @return this RestClient instance, for method chaining
     * @throws NullPointerException if decorator is null
     */
    public RestClient withDecorator(RequestDecorator decorator) {
        Objects.requireNonNull(decorator);
        this.requestDecorators.add(decorator);
        return this;
    }

    /**
     * Sets the request sender to use for sending requests.
     * <br>This method is usefull for testing when you want to mock the Http call and check request is correct.
     * @param requestSender the request sender to use or null to use the default sender
     * @return this RestClient instance, for method chaining
     */
    public RestClient withRequestSender(@Nullable RequestSender requestSender) {
        this.requestSender = requestSender == null ? this::send : requestSender;
        return this;
    }
    
    /**
     * Converts a request body into a BodyPublisher.
     * @param request the request containing the body to convert
     * @return a BodyPublisher for the request body
     * @throws IOException if there is an error during serialization
     * @throws NullPointerException if request is null
     */
    private BodyPublisher getBodyPublisher(Request request) throws IOException {
        Objects.requireNonNull(request);
        final Object body = request.getBody();
    	if (body == null) {
    		return HttpRequest.BodyPublishers.noBody();
    	} else if (body instanceof String stringBody) {
    		return BodyPublishers.ofString(stringBody);
    	} else {
    		return BodyPublishers.ofString(this.serializeRequest(body));
    	}
    }
    
    /**
     * Performs an HTTP request with built-in error handling
     * @param request the request to send (decorated by the decorators added to this client)
     * @return the response
     * @throws IOException if an I/O error occurs
     * @throws RequestException if the server returns an error response
     * @throws NullPointerException if request is null
     */
    private HttpResponse<String> send(HttpClient httpClient, Request request) throws IOException {
        // Build the request
        Builder requestBuilder = HttpRequest.newBuilder().uri(request.getUri()).timeout(Duration.ofSeconds(30));

        // Add headers
        final Builder finalRequestBuilder = requestBuilder;
        request.getHeaders().forEach((key, values) -> 
            values.forEach(value -> finalRequestBuilder.header(key, value))
        );

        // Define HTTP method and body
        requestBuilder.method(request.getMethod().name(), getBodyPublisher(request));
        
        final HttpRequest httpRequest = requestBuilder.build();
        
        HttpResponse<String> response;
        try {
            // Send the request
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            // Restore the interrupt status and wrap in an InterruptedIOException
            Thread.currentThread().interrupt();
            InterruptedIOException iioe = new InterruptedIOException("HTTP request was interrupted");
            iioe.initCause(e);
            throw iioe;
        }
        return response;
    }

    /**
     * Executes an HTTP request and deserializes the response.
     *
     * @param <T> the type of the response object
     * @param request the request to execute
     * @param responseType the Class object representing the expected response type
     * @return the deserialized response body
     * @throws IOException if there is an I/O error or the response cannot be deserialized
     * @throws RequestException if the server returns an error response
     * @throws NullPointerException if request or responseType is null
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(Request request, Class<T> responseType) throws IOException {
        // Apply decorators
        for (RequestDecorator decorator : requestDecorators) {
            request = decorator.decorate(request);
        }
        final HttpResponse<String> response = requestSender.send(httpClient, request);
        check(request, response);
        return responseType == String.class ? (T) response.body() : deserializeResponse(response.body(), responseType);
    }

    /**
     * Deserializes a response body string into an object of the specified type.
     * <br>This method is called by {@link #execute(Request, Class)} to deserialize the response body 
     * if the requested response type is not String. Its default implementation throws an exception.
     * It must be implemented by subclasses to handle the specific deserialization logic needed (e.g., JSON, XML).
     *
     * @param <T> the type of the response object
     * @param response the response body string to deserialize
     * @param responseType the Class object representing the response type
     * @return the deserialized object
     * @throws IOException if there is an error during deserialization
     * @throws NullPointerException if response or responseType is null
     */
    protected <T> T deserializeResponse(String response, Class<T> responseType) throws IOException {
        throw new UnsupportedOperationException("Deserialization to " + responseType.getName() + " is not implemented, please override this method");
    }

    protected String serializeRequest(Object request) throws IOException {
        throw new UnsupportedOperationException("Serialization of " + request.getClass().getName() + " is not implemented, please override this method");
    }

    /**
     * Checks the response for errors and throws an exception if necessary.
     * <br>Default implementation throws an exception for 4xx and 5xx status codes.
     * <br>Subclasses can override this method to provide custom error handling.
     * 
     * @param request the original request
     * @param response the response to check
     * @throws IOException if there is an error during error checking
     * @throws RequestException if the server returns an error response
     * @throws NullPointerException if request or response is null
     */
    protected void check(Request request, HttpResponse<String> response) throws IOException {
        Objects.requireNonNull(request);
        Objects.requireNonNull(response);
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new AuthenticationException(request, response);
        } else if (response.statusCode() >= 400 && response.statusCode() < 500) {
            throw new ClientErrorException(request, response);
        } else if (response.statusCode() >= 500 && response.statusCode() < 600) {
            throw new ServerErrorException(request, response);
        }
    }

    @Override
    public void close() {
        this.httpClient.close();
    }
}
