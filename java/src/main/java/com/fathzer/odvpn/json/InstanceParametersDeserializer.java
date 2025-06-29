package com.fathzer.odvpn.json;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fathzer.odvpn.DynamicDNSProvider;
import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.ObjectConfig;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.Map;

class InstanceParametersDeserializer extends JsonDeserializer<InstanceParameters> {

	@Override
    public InstanceParameters deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode root = mapper.readTree(p);

        ObjectConfig<DynamicDNSProvider> ddns = deserializeConfig(root.get("ddns"), DynamicDNSProvider.class, mapper);
        ObjectConfig<VPSProvider> vps = deserializeConfig(root.get("vps"), VPSProvider.class, mapper);
        Map<String, Object> vpn = mapper.convertValue(root.get("vpn"), new TypeReference<Map<String, Object>>() {});

        return new InstanceParameters(vps, ddns, vpn);
    }

    private <T> ObjectConfig<T> deserializeConfig(JsonNode node, Class<T> providerType, ObjectMapper mapper) throws IOException {
        JsonParser parser = node.traverse(mapper);
        ObjectConfigDeserializer<T> deserializer = new ObjectConfigDeserializer<>(providerType);
        return deserializer.deserialize(parser, mapper.getDeserializationContext());
    }
}
