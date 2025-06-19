package com.fathzer.terravpn.afraid;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;

import com.fathzer.terravpn.Configuration;
import com.fathzer.terravpn.DynamicDNSProvider;

/**
 * Afraid.org implementation of DynamicDNSProvider.
 * This provider updates DNS records using afraid.org's free DNS service.
 */
public class AfraidDDNS extends DynamicDNSProvider {
    private static final String VAR_TOKEN = "afraid_token";

    @Override
    public String id() {
        return "afraidDdns";
    }
    @Override
    public String name() {
        return "Afraid.org";
    }
    @Override
    public Optional<String> description() {
        return Optional.of("afraid.org's free dynamic DNS service");
    }

    @Override
    public void updateDns(Configuration configuration, String ip) throws IOException, InterruptedException {
        final String token = configuration.config().get(VAR_TOKEN).toString();
        final URI uri = URI.create("https://freedns.afraid.org/dynamic/update.php?" + token + "&address=" + ip);
        final HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
        final HttpResponse<String> response = doRequest(request);
        final String body = response.body();
        // Check for "address not changed" error message or "Updated" confirmation
        if (response.statusCode() != 200 || body == null || (!body.startsWith("Updated") && !body.matches("ERROR: Address \\d+\\.\\d+\\.\\d+\\.\\d+ has not changed\\."))) {
            throw new IOException("Failed to update DNS: ("+response.statusCode()+") "+body);
        }
    }
    @Override
    public Set<String> getVariables() {
        return Set.of(VAR_TOKEN);
    }
}
