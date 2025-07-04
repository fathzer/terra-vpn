package com.fathzer.odvpn.json;

import java.time.Instant;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fathzer.odvpn.repository.InstanceParameters;

public class CustomSerializationModule extends SimpleModule {
	private static final long serialVersionUID = 1L;

    public CustomSerializationModule() {
        // Register the serializer
        addSerializer(InstanceParameters.class, new InstanceParametersSerializer());
        // Register the deserializer
        addDeserializer(InstanceParameters.class, new InstanceParametersDeserializer());
        
        // Register Instant serializers
        addSerializer(Instant.class, new InstantSerializer());
        addDeserializer(Instant.class, new InstantDeserializer());
    }
}
