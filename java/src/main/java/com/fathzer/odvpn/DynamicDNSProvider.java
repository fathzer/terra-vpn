package com.fathzer.odvpn;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
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
     * @param hostName the host name
     * @param ip the IP address
     * @throws IOException if an I/O error occurs
     */
    public abstract void updateDns(Map<String, String> configuration, String hostName, String ip) throws IOException;

    public List<String> checkConfiguration(Map<String, String> configuration, String hostName) throws IOException {
        String ip;
        try {
            ip = InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            ip = "127.0.0.1";
        }
        try {
            // Try to update the DNS record to its current value. If it fails, it means the configuration is invalid.
            updateDns(configuration, hostName, ip);
            return List.of();
        } catch (IOException e) {
            return List.of(e.getMessage());
        }
    }

    /**
     * Sends a request to the DDNS provider.
     * <br>This is a utility method that can be used by the implementation of the {@link #updateDns(Map<String, String>, String hostName, String ip)} method.
     * It simply sends the request through a new {@link HttpClient} and returns the response after closing the client.
     * @param request the request
     * @return the response
     * @throws IOException if an I/O error occurs
     */
    protected HttpResponse<String> doRequest(final HttpRequest request) throws IOException {
        try (final HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
    }
}
