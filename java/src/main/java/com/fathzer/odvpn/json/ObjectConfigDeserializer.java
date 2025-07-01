package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
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
        JsonNode root;
        try {
            root = mapper.readTree(p);
        } catch (JsonProcessingException e) {
            throw new InvalidFormatException(p, "Invalid JSON: " + e.getMessage(), null, ObjectConfig.class);
        }

        if (root == null || !root.isObject()) {
            throw new InvalidFormatException(p, "Invalid configuration: expected JSON object", root, ObjectConfig.class);
        }

        JsonNode providerNode = root.get("providerId");
        if (providerNode == null || providerNode.isNull() || !providerNode.isTextual() || providerNode.asText().isBlank()) {
            throw new InvalidFormatException(p, "Missing or invalid providerId", providerNode, String.class);
        }
        String providerId = providerNode.asText();

        T provider = ProviderRegistry.getProvider(providerId, providerClass);
        if (provider == null) {
            throw new InvalidFormatException(p, "Unknown provider: " + providerId, providerId, providerClass);
        }

        JsonNode configNode = root.get("config");
        if (configNode == null || configNode.isNull()) {
            throw new InvalidFormatException(p, "Missing configuration", null, Map.class);
        }

        try {
            Map<String, String> config = mapper.convertValue(configNode, new TypeReference<>() {});
            return new ObjectConfig<>(provider, config);
        } catch (IllegalArgumentException e) {
            throw InvalidFormatException.from(p, "Invalid configuration: " + e.getMessage(), configNode, Map.class);
        }
    }
}