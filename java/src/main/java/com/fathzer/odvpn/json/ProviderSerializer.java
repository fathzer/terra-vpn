package com.fathzer.odvpn.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fathzer.odvpn.Provider;

public class ProviderSerializer extends JsonSerializer<Provider<?>> {
    
    @Override
    public void serialize(Provider<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("providerId", value.id());
        gen.writeObjectField("config", value.getSettings());
        gen.writeEndObject();
    }
}
