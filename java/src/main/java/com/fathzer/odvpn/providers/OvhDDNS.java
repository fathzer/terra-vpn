package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import com.fathzer.odvpn.DynamicDNSProvider;
import com.fathzer.odvpn.utils.Registerable;

/**
 * OVH DynHost implementation of DynamicDNSProvider.
 * This provider updates DNS records using OVH's DynHost service.
 */
@Registerable(
        value = "ovh",
        classes = {DynamicDNSProvider.class}
)
public class OvhDDNS extends DynamicDNSProvider {
    static final String VAR_USER = "user";
    static final String VAR_PASSWORD = "password";
    
    @Override
    public String name() {
        return "OVH DynHost";
    }

    @Override
    public void updateDns(Map<String, String> configuration, String hostName, String ip) throws IOException, InterruptedException {
        final String user = configuration.get(VAR_USER);
        final String password = configuration.get(VAR_PASSWORD);

        final String url = String.format("https://www.ovh.com/nic/update?system=dyndns&hostname=%s&myip=%s", hostName, ip);
        final URI uri = URI.create(url);
        final String authent = user + ":" + password;
        final String authentBase64 = Base64.getEncoder().encodeToString(authent.getBytes(StandardCharsets.UTF_8));
        final HttpRequest request = HttpRequest.newBuilder().uri(uri).header("Authorization", "Basic " + authentBase64).build();
        final HttpResponse<String> response = doRequest(request);
        final String body = response.body();
        // Check for "address not changed" error message or "Updated" confirmation
        if (response.statusCode() != 200 || body == null || (!body.startsWith("good") && !body.startsWith("nochg"))) {
            throw new IOException("Failed to update DNS: ("+response.statusCode()+") "+body);
        }
    }
    
    @Override
    public Set<String> getVariables() {
        return Set.of(VAR_USER, VAR_PASSWORD);
    }
}
