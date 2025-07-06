package com.fathzer.odvpn.repository;

import com.fathzer.odvpn.DynamicDNSProvider;
import com.fathzer.odvpn.VPSProvider;

public record InstanceParameters(
    ObjectConfig<VPSProvider> vps,
    ObjectConfig<DynamicDNSProvider> ddns,
    VPNConfig vpn) {
}
