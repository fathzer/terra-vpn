package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.VPNConfig;

import java.io.IOException;

public class InstanceParametersSerializer extends JsonSerializer<InstanceParameters> {
    
    @Override
    public void serialize(InstanceParameters value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        
        gen.writeFieldName("ddns");
        new ProviderSerializer().serialize(value.ddns(), gen, serializers);

        gen.writeFieldName("vps");
        new ProviderSerializer().serialize(value.vps(), gen, serializers);
        
        JsonSerializer<VPNConfig> vpnSerializer = new VPNConfigSerializer();
        gen.writeFieldName("vpn");
        vpnSerializer.serialize(value.vpn(), gen, serializers);
        
        gen.writeEndObject();
    }
}
