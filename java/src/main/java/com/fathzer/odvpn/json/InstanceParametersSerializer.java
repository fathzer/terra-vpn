package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fathzer.odvpn.DynamicDNSProvider;
import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.ObjectConfig;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.utils.Registerable;

import java.io.IOException;
import java.util.function.Function;

public class InstanceParametersSerializer extends JsonSerializer<InstanceParameters> {

    private <T> Function<T, String> createProviderId() {
        return provider -> {
            Registerable registerable = provider.getClass().getAnnotation(Registerable.class);
            if (registerable == null) {
                throw new IllegalArgumentException("Provider " + provider.getClass().getName() + " is not registered");
            }
            return registerable.value();
        };
    }
    
    @Override
    public void serialize(InstanceParameters value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        
        // Create serializers with type-safe providerId functions
        JsonSerializer<ObjectConfig<DynamicDNSProvider>> ddnsSerializer = new ObjectConfigSerializer<>(createProviderId());
        JsonSerializer<ObjectConfig<VPSProvider>> vpsSerializer = new ObjectConfigSerializer<>(createProviderId());
        JsonSerializer<VPNConfig> vpnSerializer = new VPNConfigSerializer();

        gen.writeFieldName("ddns");
        ddnsSerializer.serialize(value.ddns(), gen, serializers);

        gen.writeFieldName("vps");
        vpsSerializer.serialize(value.vps(), gen, serializers);
        
        gen.writeFieldName("vpn");
        vpnSerializer.serialize(value.vpn(), gen, serializers);
        
        gen.writeEndObject();
    }
}
