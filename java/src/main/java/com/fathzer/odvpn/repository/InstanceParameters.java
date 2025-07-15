package com.fathzer.odvpn.repository;

import com.fathzer.odvpn.DynamicDNSProvider;
import com.fathzer.odvpn.VPSProvider;

public record InstanceParameters(
    VPSProvider<?> vps,
    DynamicDNSProvider<?> ddns,
    VPNConfig vpn) {
}
