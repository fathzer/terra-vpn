package com.fathzer.odvpn.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.repository.VPNConfig.Protocol;

public class VPNConfigSerializer extends JsonSerializer<VPNConfig> {
    @Override
    public void serialize(VPNConfig value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("hostname", value.hostname());
        
        if (value.dnsServers() != null && !value.dnsServers().isEmpty()) {
            gen.writeArrayFieldStart("dnsServers");
            for (String dns : value.dnsServers()) {
                gen.writeString(dns);
            }
            gen.writeEndArray();
        }
        
        if (Protocol.UDP != value.protocol()) {
            gen.writeStringField("protocol", value.protocol().name().toLowerCase());
        }
        
        if (value.port() != 1194) {
            gen.writeNumberField("port", value.port());
        }
        
        gen.writeEndObject();
    }
}
