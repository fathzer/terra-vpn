package com.fathzer.odvpn.json;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.repository.InstanceParameters;

public class InstanceParametersParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private InstanceParametersParser() {
    }

    static {
        MAPPER.registerModule(new CustomSerializationModule());
    }

    public static InstanceParameters read(Path path) throws IOException {
        try {
            return MAPPER.readValue(path.toFile(), InstanceParameters.class);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    public static void write(Path path, InstanceParameters config) throws IOException {
        try {
            MAPPER.writeValue(path.toFile(), config);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }
}
