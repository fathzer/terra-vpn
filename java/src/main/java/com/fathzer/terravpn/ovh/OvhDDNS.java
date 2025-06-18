package com.fathzer.terravpn.ovh;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

import com.fathzer.terravpn.Configuration;
import com.fathzer.terravpn.DynamicDNSProvider;

/**
 * OVH DynHost implementation of DynamicDNSProvider.
 * This provider updates DNS records using OVH's DynHost service.
 */
public class OvhDDNS extends DynamicDNSProvider {
    @Override
    public String id() {
        return "ovhDdns";
    }
    @Override
    public String name() {
        return "OVH DynHost";
    }
    @Override
    public Optional<String> description() {
        return Optional.of("OVH's DynHost service for dynamic DNS updates");
    }

    @Override
    public void updateDns(Configuration configuration, String ip) throws IOException, InterruptedException {
        final String user = configuration.config().get("ovhDdns_user").toString();
        final String password = configuration.config().get("ovhDdns_password").toString();

        final String hostname = configuration.config().get("ddns_hostname").toString();
        final String url = String.format("https://www.ovh.com/nic/update?system=dyndns&hostname=%s&myip=%s", hostname, ip);
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

    public static void main(String[] args) throws Exception {
        final OvhDDNS ovhDDNS = new OvhDDNS();
        Configuration config = Configuration.fromJson(Paths.get("java/configScalewayOvh.json"));
        ovhDDNS.updateDns(config, "127.0.0.1");
    }
}
