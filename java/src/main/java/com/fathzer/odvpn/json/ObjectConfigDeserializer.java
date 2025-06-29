package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fathzer.odvpn.ProviderRegistry;
import com.fathzer.odvpn.repository.ObjectConfig;

import java.io.IOException;
import java.util.Map;

public class ObjectConfigDeserializer<T> extends JsonDeserializer<ObjectConfig<T>> {
    private final Class<T> providerClass;

    protected ObjectConfigDeserializer(Class<T> providerClass) {
        this.providerClass = providerClass;
    }

    @Override
    public ObjectConfig<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode root = mapper.readTree(p);

        String providerId = root.get("providerId").asText();
        T provider = ProviderRegistry.getProvider(providerId, providerClass);

        JsonNode configNode = root.get("config");
        Map<String, String> config = mapper.convertValue(configNode, new TypeReference<>() {});

        return new ObjectConfig<>(provider, config);
    }
}