package com.fathzer.odvpn.json;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.repository.InstanceParameters;

class InstanceParametersDeserializerTest {
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new CustomSerializationModule());
    }
    
    @Test
    void test() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("configDigitalOceanAfraid.json")) {
            InstanceParameters params = mapper.readValue(is, InstanceParameters.class);
            assertEquals("DigitalOceanVPS", params.vps().provider().getClass().getSimpleName());
            assertEquals("digitalOceanToken", params.vps().config().get("token"));
            assertEquals("lon1", params.vps().config().get("zone"));
            assertEquals("AfraidDDNS", params.ddns().provider().getClass().getSimpleName());
            assertEquals("afraidToken", params.ddns().config().get("token"));
            assertEquals("terravpn.mydomain.com", params.vpn().get("hostname"));
            assertEquals(List.of("86.54.11.100", "86.54.11.200"), params.vpn().get("dnsServers"));
        }
    }

}
