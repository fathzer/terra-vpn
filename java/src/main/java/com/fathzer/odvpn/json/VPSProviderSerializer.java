package com.fathzer.odvpn.json;

import java.io.IOException;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fathzer.odvpn.VPSProvider;

public class VPSProviderSerializer extends JsonSerializer<VPSProvider<?>> {
    private final Function<VPSProvider<?>, String> providerId;

    VPSProviderSerializer(Function<VPSProvider<?>, String> providerId) {
        this.providerId = providerId;
    }
    
    @Override
    public void serialize(VPSProvider<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("providerId", providerId.apply(value));
        gen.writeObjectField("config", value.getSettings());
        gen.writeEndObject();
    }
}
