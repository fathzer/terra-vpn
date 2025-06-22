package com.fathzer.terravpn.repository;

import java.util.Map;

import com.fathzer.terravpn.DynamicDNSProvider;
import com.fathzer.terravpn.VPSProvider;

public record InstanceParameters(
    ObjectConfig<VPSProvider> vps,
    ObjectConfig<DynamicDNSProvider> ddns,
    Map<String, Object> vpn) {

    private static final String HOST_NAME_KEY = "hostname";
    private static final String DNS_SERVERS_KEY = "dns_servers";
    private static final String PROTOCOL_KEY = "protocol";
    private static final String PORT_KEY = "port";

    public String hostName() {
        return (String) vpn.get(HOST_NAME_KEY);
    }

    public String[] dnsServers() {
        return (String[]) vpn.get(DNS_SERVERS_KEY);
    }

    public String protocol() {
        return (String) vpn.getOrDefault    (PROTOCOL_KEY, "udp");
    }

    public int port() {
        return (int) vpn.getOrDefault(PORT_KEY, 1194);
    }
}
