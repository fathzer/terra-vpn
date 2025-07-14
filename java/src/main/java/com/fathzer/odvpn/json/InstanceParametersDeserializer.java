package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fathzer.odvpn.DynamicDNSProvider;
import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.repository.ObjectConfig;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;

class InstanceParametersDeserializer extends JsonDeserializer<InstanceParameters> {

    @Override
    public InstanceParameters deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode root;
        try {
            root = mapper.readTree(p);
        } catch (JsonProcessingException e) {
            throw new InvalidFormatException(p, "Invalid JSON: " + e.getMessage(), null, InstanceParameters.class);
        }

        if (root == null || !root.isObject()) {
            throw new InvalidFormatException(p, "Invalid configuration: expected JSON object", root, InstanceParameters.class);
        }

        JsonNode ddnsNode = root.get("ddns");
        if (ddnsNode == null || ddnsNode.isNull()) {
            throw new InvalidFormatException(p, "Missing ddns configuration", null, ObjectConfig.class);
        }
        ObjectConfig<DynamicDNSProvider> ddns = deserializeConfig(ddnsNode, DynamicDNSProvider.class, mapper);

        JsonNode vpsNode = root.get("vps");
        if (vpsNode == null || vpsNode.isNull()) {
            throw new InvalidFormatException(p, "Missing vps configuration", null, VPSProvider.class);
        }
        VPSProvider<?> vps;
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            VPSProviderDeserializer<?> deserializer = new VPSProviderDeserializer(VPSProvider.class);
            vps = deserializer.deserialize(vpsNode.traverse(mapper), mapper.getDeserializationContext());
        } catch (Exception e) {
            throw InvalidFormatException.from(p, "Invalid vps configuration: " + e.getMessage(), vpsNode, VPSProvider.class);
        }

        JsonNode vpnNode = root.get("vpn");
        if (vpnNode == null || vpnNode.isNull()) {
            throw new InvalidFormatException(p, "Missing vpn configuration", null, VPNConfig.class);
        }
        VPNConfig vpn;
        try {
            vpn = new VPNConfigDeserializer().deserialize(vpnNode.traverse(mapper), mapper.getDeserializationContext());
        } catch (IllegalArgumentException e) {
            throw InvalidFormatException.from(p, "Invalid vpn configuration: " + e.getMessage(), vpnNode, VPNConfig.class);
        }

        return new InstanceParameters(vps, ddns, vpn);
    }

        private <T> ObjectConfig<T> deserializeConfig(JsonNode node, Class<T> providerType, ObjectMapper mapper) throws IOException {
        try {
            ObjectConfigDeserializer<T> deserializer = new ObjectConfigDeserializer<>(providerType);
            return deserializer.deserialize(node.traverse(mapper), mapper.getDeserializationContext());
        } catch (Exception e) {
            throw InvalidFormatException.from(null, "Failed to deserialize configuration: " + e.getMessage(), node, providerType);
        }
    }
}
