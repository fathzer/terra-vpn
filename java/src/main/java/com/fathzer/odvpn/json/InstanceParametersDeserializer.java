package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fathzer.odvpn.DynamicDNSProvider;
import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.repository.InstanceParameters;
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

        DynamicDNSProvider<?> ddns = deserializeProvider(p, root.get("ddns"), "ddns", DynamicDNSProvider.class);
        VPSProvider<?> vps = deserializeProvider(p, root.get("vps"), "vps",VPSProvider.class);

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T deserializeProvider(JsonParser p, JsonNode node, String configName, Class<T> providerClass) throws IOException {
        if (node == null || node.isNull()) {
            throw new InvalidFormatException(p, "Missing " + configName + " configuration", null, providerClass);
        }
        try {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            // Create a raw type instance of ProviderDeserializer to avoid type parameter issues
            ProviderDeserializer deserializer = new ProviderDeserializer(providerClass);
            // The deserializer returns Provider<T>, but we need to cast to T
            return (T) deserializer.deserialize(node.traverse(mapper), mapper.getDeserializationContext());
        } catch (Exception e) {
            throw InvalidFormatException.from(p, "Invalid " + configName + " configuration: " + e.getMessage(), node, providerClass);
        }
    }
}
