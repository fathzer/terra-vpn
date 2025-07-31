package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fathzer.odvpn.providers.PermanentVPS;
import com.fathzer.odvpn.DynamicDNSProvider;
import com.fathzer.odvpn.providers.AfraidDDNS;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.VPNConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class InstanceParametersSerDeTest {

    private static class NotRegisterable extends DynamicDNSProvider<String> {
        @Override
        public void updateDns(String hostName, String ip) throws IOException {
            // Do nothing
        }

        @Override
        public String name() {
            return "not registerable";
        }

        @Override
        public Class<String> getConfigClass() {
            return String.class;
        }
    }

    private static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new CustomSerializationModule());
        return mapper;
    }

    @Test
    void testSerializeFullInstanceParameters() throws JsonProcessingException {
        var vps = new PermanentVPS();
        vps.setSettings(new PermanentVPS.PermanentSettings("1.2.3.4", "user"));
        var ddns = new AfraidDDNS();
        ddns.setSettings(new AfraidDDNS.Settings("token123"));
        var vpn = new VPNConfig("vpn.example.com", Arrays.asList("8.8.8.8","9.9.9.9"), VPNConfig.Protocol.TCP, 443);
        String json = getMapper().writeValueAsString(new InstanceParameters(vps, ddns, vpn));

        InstanceParameters params = getMapper().readValue(json, InstanceParameters.class);
        assertTrue(params.vps() instanceof PermanentVPS);
        assertTrue(params.ddns() instanceof AfraidDDNS);
        assertEquals("token123", ((AfraidDDNS)params.ddns()).getSettings().token());
        assertEquals(vpn, params.vpn());
    }

    @Test
    void testDeserializeInvalidVpnThrows() {
        String json = """
        {
            "vps": {"providerId": "permanent", "config": {"ip": "1.2.3.4", "sshUser": "user"}},
            "ddns": {"providerId": "afraid", "config": {"token": "token123"}}
        }
        """;
        assertThrows(InvalidFormatException.class, () -> getMapper().readValue(json, InstanceParameters.class));

        String nullVpnJson = """
        {
            "vps": {"providerId": "permanent", "config": {"ip": "1.2.3.4", "sshUser": "user"}},
            "ddns": {"providerId": "afraid", "config": {"token": "token123"}},
            "vpn": null
        }
        """;
        assertThrows(InvalidFormatException.class, () -> getMapper().readValue(nullVpnJson, InstanceParameters.class));

        String invalidVpnJson = """
        {
            "vps": {"providerId": "permanent", "config": {"ip": "1.2.3.4", "sshUser": "user"}},
            "ddns": {"providerId": "afraid", "config": {"token": "token123"}},
            "vpn": {"arg":"toto"}
        }
        """;
        assertThrows(InvalidFormatException.class, () -> getMapper().readValue(invalidVpnJson, InstanceParameters.class));
    }

    @Test
    void testDeserializeInvalidProviderThrows() {
    	// Unknown provider
        String json = """
        {
            "vps": {"providerId": "invalid-vps", "config": {"token": "token123"}},
            "ddns": {"providerId": "afraid", "config": {"token": "token123"}},
            "vpn": {"hostname": "vpn.example.com"}
        }
        """;
        assertThrows(InvalidFormatException.class, () -> getMapper().readValue(json, InstanceParameters.class));
        
        // No Provider
        String noProviderJson = """
        {
            "ddns": {"providerId": "afraid", "config": {"token": "token123"}},
            "vpn": {"hostname": "vpn.example.com"}
        }
        """;
        assertThrows(InvalidFormatException.class, () -> getMapper().readValue(noProviderJson, InstanceParameters.class));
        
        // Null Provider
        String nullProviderJson = """
        {
            "vps": null,
            "ddns": {"providerId": "afraid", "config": {"token": "token123"}},
            "vpn": {"hostname": "vpn.example.com"}
        }
        """;
        assertThrows(InvalidFormatException.class, () -> getMapper().readValue(nullProviderJson, InstanceParameters.class));
    }
    
    @Test
    void testInvalidJSon() throws IOException {
    	assertNull(getMapper().readValue("null", InstanceParameters.class));
    	
    	
        String jsonMissingComma = """
        {
            "vps": {"providerId": "invalid-vps", "config": {"token": "token123"}}
            "ddns": {"providerId": "afraid", "config": {"token": "token123"}},
            "vpn": {"hostname": "vpn.example.com"}
        }
        """;
        assertThrows(InvalidFormatException.class, () -> getMapper().readValue(jsonMissingComma, InstanceParameters.class));
        
        String notObjectJson = "[\"titi\", \"toto\"]";
        assertThrows(InvalidFormatException.class, () -> getMapper().readValue(notObjectJson, InstanceParameters.class));
    }

    @Test
    void testDeSerializeNotRegisterableProvider() {
        ObjectMapper mapper = getMapper();
        var vps = new PermanentVPS();
        vps.setSettings(new PermanentVPS.PermanentSettings("1.2.3.4", "user"));
        var ddns = new NotRegisterable();
        var vpn = new VPNConfig("vpn.example.com", Collections.emptyList(), VPNConfig.Protocol.TCP, 443);
        InstanceParameters value = new InstanceParameters(vps, ddns, vpn);
        assertThrows(JsonProcessingException.class, () -> mapper.writeValueAsString(value));
    }
}
