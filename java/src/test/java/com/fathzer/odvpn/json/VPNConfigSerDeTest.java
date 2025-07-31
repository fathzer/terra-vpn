package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.repository.VPNConfig.Protocol;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class VPNConfigSerDeTest {
    private static ObjectMapper getMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(VPNConfig.class, new VPNConfigSerializer());
        module.addDeserializer(VPNConfig.class, new VPNConfigDeserializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        return mapper;
    }

    @Test
    void testSerializeFullVPNConfig() throws JsonProcessingException {
        ObjectMapper mapper = getMapper();
        VPNConfig config = new VPNConfig(
                "vpn.example.com",
                Arrays.asList("8.8.8.8", "1.1.1.1"),
                Protocol.TCP,
                443
        );
        String json = mapper.writeValueAsString(config);
        assertTrue(json.contains("\"hostname\":\"vpn.example.com\""));
        assertTrue(json.contains("\"dnsServers\":[\"8.8.8.8\",\"1.1.1.1\"]"));
        assertTrue(json.contains("\"protocol\":\"tcp\""));
        assertTrue(json.contains("\"port\":443"));
    }

    @Test
    void testSerializeMinimalVPNConfig() throws JsonProcessingException {
        ObjectMapper mapper = getMapper();
        VPNConfig config = new VPNConfig(
                "vpn.example.com",
                null,
                null,
                0
        );
        String json = mapper.writeValueAsString(config);
        assertTrue(json.contains("\"hostname\":\"vpn.example.com\""));
        assertFalse(json.contains("dnsServers"));
        assertFalse(json.contains("protocol"));
        assertFalse(json.contains("port"));
    }

    @Test
    void testDeserializeFullVPNConfig() throws IOException {
        ObjectMapper mapper = getMapper();
        String json = "{" +
                "\"hostname\":\"vpn.example.com\"," +
                "\"dnsServers\":[\"8.8.8.8\",\"1.1.1.1\"]," +
                "\"protocol\":\"tcp\"," +
                "\"port\":443" +
                "}";
        VPNConfig config = mapper.readValue(json, VPNConfig.class);
        assertEquals("vpn.example.com", config.hostname());
        assertEquals(Arrays.asList("8.8.8.8", "1.1.1.1"), config.dnsServers());
        assertEquals(Protocol.TCP, config.protocol());
        assertEquals(443, config.port());
    }

    @Test
    void testDeserializeMinimalVPNConfig() throws IOException {
        ObjectMapper mapper = getMapper();
        String json = "{" +
                "\"hostname\":\"vpn.example.com\"" +
                "}";
        VPNConfig config = mapper.readValue(json, VPNConfig.class);
        assertEquals("vpn.example.com", config.hostname());
        assertNull(config.dnsServers());
        assertEquals(Protocol.UDP, config.protocol()); // default
        assertEquals(1194, config.port()); // default
    }

    @Test
    void testDeserializeInvalidProtocol() {
        ObjectMapper mapper = getMapper();
        String json = "{" +
                "\"hostname\":\"vpn.example.com\"," +
                "\"protocol\":\"invalid\"" +
                "}";
        assertThrows(Exception.class, () -> mapper.readValue(json, VPNConfig.class));
    }

    @Test
    void testDeserializeNull() throws IOException {
        ObjectMapper mapper = getMapper();
        String json = "null";
        VPNConfig config = mapper.readValue(json, VPNConfig.class);
        assertNull(config);
    }
}
