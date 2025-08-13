package com.fathzer.odvpn.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.VPSProvider;

public abstract class VPSProviderTestUtils {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends VPSProvider<?>> T deserialize(ObjectMapper mapper, String json) throws IOException {
        return (T)new ProviderDeserializer<>((Class)VPSProvider.class).deserialize(mapper.getFactory().createParser(json), null);
    }
}
