package com.fathzer.odvpn.json;

import java.io.IOException;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fathzer.odvpn.Provider;

public class ProviderSerializer extends JsonSerializer<Provider<?>> {
    private final Function<Provider<?>, String> providerId;

    ProviderSerializer(Function<Provider<?>, String> providerId) {
        this.providerId = providerId;
    }
    
    @Override
    public void serialize(Provider<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("providerId", providerId.apply(value));
        gen.writeObjectField("config", value.getSettings());
        gen.writeEndObject();
    }
}
