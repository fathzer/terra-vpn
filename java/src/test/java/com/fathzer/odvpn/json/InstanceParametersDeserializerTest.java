package com.fathzer.odvpn.json;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.providers.AfraidDDNS;
import com.fathzer.odvpn.providers.utils.BasicTokenAuthVPSSettings;
import com.fathzer.odvpn.repository.InstanceParameters;

class InstanceParametersDeserializerTest {
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new CustomSerializationModule());
    }
    
    @Test
    void test() throws Exception {
        String json = """
        {
            "vps" : {
                "providerId": "digitalOcean",
                "config": {
                    "token": "digitalOceanToken",
                    "instanceType": "s-1vcpu-1gb",
                    "region": "lon1"
                }
            },
            "ddns": {
                "providerId": "afraid",
                "config": {
                    "token": "afraidToken"
                }
            },
            "vpn": {
                "hostname": "terravpn.mydomain.com",
                "dnsServers": ["86.54.11.100","86.54.11.200"]
            }
        }
        """;
        InstanceParameters params = mapper.readValue(json, InstanceParameters.class);
        assertEquals("DigitalOceanVPS", params.vps().getClass().getSimpleName());
        BasicTokenAuthVPSSettings vpsSettings = (BasicTokenAuthVPSSettings) params.vps().getSettings();
        assertEquals("digitalOceanToken", vpsSettings.getToken());
        assertEquals("lon1", vpsSettings.getRegion("zone"));
        assertEquals("AfraidDDNS", params.ddns().getClass().getSimpleName());
        AfraidDDNS ddns = (AfraidDDNS) params.ddns();
        assertEquals("afraidToken", ddns.getSettings().token());
        assertEquals("terravpn.mydomain.com", params.vpn().hostname());
        assertEquals(Set.of("86.54.11.100", "86.54.11.200"), new HashSet<>(params.vpn().dnsServers()));
    }
}
