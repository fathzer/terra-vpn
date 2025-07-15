package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fathzer.odvpn.ProviderRegistry;
import com.fathzer.odvpn.Provider;
import java.io.IOException;
import java.util.Map;

public class ProviderDeserializer<T> extends JsonDeserializer<Provider<T>> {
    private final Class<Provider<T>> providerClass;

    protected ProviderDeserializer(Class<Provider<T>> providerClass) {
        this.providerClass = providerClass;
    }

    @Override
    public Provider<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode root;
        try {
            root = mapper.readTree(p);
        } catch (JsonProcessingException e) {
            throw new InvalidFormatException(p, "Invalid JSON: " + e.getMessage(), null, Provider.class);
        }

        if (root == null || !root.isObject()) {
            throw new InvalidFormatException(p, "Invalid configuration: expected JSON object", root, Provider.class);
        }

        JsonNode providerNode = root.get("providerId");
        if (providerNode == null || providerNode.isNull() || !providerNode.isTextual() || providerNode.asText().isBlank()) {
            throw new InvalidFormatException(p, "Missing or invalid providerId", providerNode, String.class);
        }
        String providerId = providerNode.asText();

        Provider<T> provider = ProviderRegistry.getProvider(providerId, providerClass);
        if (provider == null) {
            throw new InvalidFormatException(p, "Unknown provider: " + providerId, providerId, providerClass);
        }

        JsonNode configNode = root.get("config");
        if (configNode == null || configNode.isNull()) {
            throw new InvalidFormatException(p, "Missing configuration", null, Map.class);
        }

        try {
            provider.setSettings(mapper.convertValue(configNode, provider.getConfigClass()));
            return provider;
        } catch (IllegalArgumentException e) {
            throw InvalidFormatException.from(p, "Invalid configuration: " + e.getMessage(), configNode, Map.class);
        }
    }
}