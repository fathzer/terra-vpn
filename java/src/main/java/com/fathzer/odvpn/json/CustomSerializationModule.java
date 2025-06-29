package com.fathzer.odvpn.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fathzer.odvpn.repository.InstanceParameters;

public class CustomSerializationModule extends SimpleModule {
	private static final long serialVersionUID = 1L;

    public CustomSerializationModule() {
        // Register the serializer
        addSerializer(InstanceParameters.class, new InstanceParametersSerializer());
        // Register the deserializer
        addDeserializer(InstanceParameters.class, new InstanceParametersDeserializer());
    }
}
