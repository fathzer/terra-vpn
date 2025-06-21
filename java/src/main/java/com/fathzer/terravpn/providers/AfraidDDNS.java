package com.fathzer.terravpn.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;

import com.fathzer.terravpn.DynamicDNSProvider;
import com.fathzer.terravpn.utils.Registerable;

/**
 * Afraid.org implementation of DynamicDNSProvider.
 * This provider updates DNS records using afraid.org's free DNS service.
 */
@Registerable(
        value = "afraid",
        classes = {DynamicDNSProvider.class}  // classes to register
)
public class AfraidDDNS extends DynamicDNSProvider {
    static final String VAR_TOKEN = "token";

    @Override
    public String name() {
        return "Afraid.org";
    }

    @Override
    public void updateDns(Map<String, String> configuration, String hostName, String ip) throws IOException, InterruptedException {
        final String token = configuration.get(VAR_TOKEN);
        final URI uri = URI.create("https://freedns.afraid.org/dynamic/update.php?" + token + "&address=" + ip);
        final HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
        final HttpResponse<String> response = doRequest(request);
        final String body = response.body();
        final boolean httpErr = response.statusCode() != 200;
        // Check for "address not changed" error message or "Updated" confirmation
        if (httpErr || body == null || !isBodyOk(body, ip)) {
            throw new IOException("Failed to update DNS: ("+response.statusCode()+") "+body);
        }
    }

    private boolean isBodyOk(String body, String ip) {
        body = body.replaceAll("[\r\n]", "");
        final boolean updated = body.startsWith("Updated");
        final boolean notChanged = body.endsWith(String.format("ERROR: Address %s has not changed.", ip));
        return updated || notChanged;
    }
    @Override
    public Set<String> getVariables() {
        return Set.of(VAR_TOKEN);
    }
}
