package com.fathzer.terravpn;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;

/**
 * Abstract Dynamic DNS provider.
 */
public abstract class DynamicDNSProvider implements Provider {

    /**
     * Gets the name of variables specifically required to configure this provider.
     * @return the variables required to configure this provider.
     */
    public abstract Set<String> getVariables();

    /**
     * Updates the DNS record.
     * @param configuration the configuration
     * @param ip the IP address
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    public abstract void updateDns(Map<String, String> configuration, String hostName,String ip) throws IOException, InterruptedException;

    /**
     * Sends a request to the DDNS provider.
     * <br>This is a utility method that can be used by the implementation of the {@link #updateDns(Map<String, String>, String hostName,String)} method.
     * It simply sends the request through a new {@link HttpClient} and returns the response after closing the client.
     * @param request the request
     * @return the response
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    protected HttpResponse<String> doRequest(final HttpRequest request) throws IOException, InterruptedException {
        try (final HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
