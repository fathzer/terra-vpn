package com.fathzer.terravpn.repository;

import java.util.Map;

import com.fathzer.terravpn.DynamicDNSProvider;
import com.fathzer.terravpn.VPSProvider;

public record InstanceParameters(
    ObjectConfig<VPSProvider> vps,
    ObjectConfig<DynamicDNSProvider> ddns,
    Map<String, Object> vpn) {

    private static final String HOST_NAME_KEY = "hostname";

    public String getHostName() {
        return (String) vpn.get(HOST_NAME_KEY);
    }

    public String[] getDnsServers() {
        return (String[]) vpn.get("dns_servers");
    }

    public String getProtocol() {
        return (String) vpn.get("protocol");
    }

    public int getPort() {
        return (int) vpn.get("port");
    }
}
