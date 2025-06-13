package com.fathzer.terravpn.ovh;

import java.util.Optional;

import com.fathzer.terravpn.DynamicDNSProvider;

/**
 * OVH DynHost implementation of DynamicDNSProvider.
 * This provider updates DNS records using OVH's DynHost service.
 */
public class OvhDDNS implements DynamicDNSProvider {
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
    public String getAuthentArguments() {
        return "'${var.dynhost_user}' '${var.dynhost_password}'";
    }
 }
