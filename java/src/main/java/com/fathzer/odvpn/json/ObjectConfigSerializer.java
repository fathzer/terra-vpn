package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fathzer.odvpn.repository.ObjectConfig;

import java.io.IOException;
import java.util.function.Function;

public class ObjectConfigSerializer<T> extends JsonSerializer<ObjectConfig<T>> {
    private final Function<T, String> providerId;

    ObjectConfigSerializer(Function<T, String> providerId) {
        this.providerId = providerId;
    }

    @Override
    public void serialize(ObjectConfig<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("providerId", providerId.apply(value.provider()));
        gen.writeObjectField("config", value.rawConfig());
        gen.writeEndObject();
    }
}
