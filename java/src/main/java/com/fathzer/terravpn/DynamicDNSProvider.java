package com.fathzer.terravpn;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Abstract Dynamic DNS provider.
 */
public abstract class DynamicDNSProvider implements Provider {

    public abstract void updateDns(Configuration configuration, String ip) throws IOException, InterruptedException;

    protected HttpResponse<String> doRequest(final HttpRequest request) throws IOException, InterruptedException {
        try (final HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
