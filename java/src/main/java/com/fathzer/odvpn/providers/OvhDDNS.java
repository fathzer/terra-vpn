package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
public class OvhDDNS extends DynamicDNSProvider<OvhDDNS.Settings> {
    public static record Settings(String user, String password) {}
    
    @Override
    public String name() {
        return "OVH DynHost";
    }

    @Override
    public void updateDns(String hostName, String ip) throws IOException {
        final String url = String.format("https://www.ovh.com/nic/update?system=dyndns&hostname=%s&myip=%s", hostName, ip);
        final URI uri = URI.create(url);
        final String authent = resolve(settings.user()) + ":" + resolve(settings.password());
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
    public Class<Settings> getConfigClass() {
        return Settings.class;
    }
}
