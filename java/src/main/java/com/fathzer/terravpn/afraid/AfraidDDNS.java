package com.fathzer.terravpn.afraid;

import java.util.Optional;

import com.fathzer.terravpn.DynamicDNSProvider;

/**
 * Afraid.org implementation of DynamicDNSProvider.
 * This provider updates DNS records using afraid.org's free DNS service.
 */
public class AfraidDDNS implements DynamicDNSProvider {
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
    public String getAuthentArguments() {
        return "'${var.afraid_token}'";
    }
}
