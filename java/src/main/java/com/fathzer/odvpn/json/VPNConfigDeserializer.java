package com.fathzer.odvpn.json;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fathzer.odvpn.repository.VPNConfig;

public class VPNConfigDeserializer extends JsonDeserializer<VPNConfig> {
    @Override
    public VPNConfig deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        String hostName = node.get("hostname").asText();
        
        JsonNode dnsNode = node.get("dnsServers");
        List<String> dnsServers = null;
        if (dnsNode != null && dnsNode.isArray()) {
            dnsServers = new LinkedList<>();
            for (int i = 0; i < dnsNode.size(); i++) {
                dnsServers.add(dnsNode.get(i).asText());
            }
        }
        
        VPNConfig.Protocol protocol = null;
        JsonNode protocolNode = node.get("protocol");
        if (protocolNode != null) {
            String protocolStr = protocolNode.asText();
            try {
                protocol = VPNConfig.Protocol.valueOf(protocolStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid protocol: " + protocolStr + ". Must be one of: " + 
                    Arrays.toString(VPNConfig.Protocol.values()));
            }
        }
        
        int port = node.has("port") ? node.get("port").asInt() : 0;
        
        return new VPNConfig(hostName, dnsServers, protocol, port);
    }
}
