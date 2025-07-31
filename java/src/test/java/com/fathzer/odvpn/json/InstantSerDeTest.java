package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InstantSerDeTest {
    private static ObjectMapper getMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new InstantSerializer());
        module.addDeserializer(Instant.class, new InstantDeserializer());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        return mapper;
    }

    @Test
    void testSerializeInstant() throws JsonProcessingException {
        ObjectMapper mapper = getMapper();
        Instant instant = Instant.parse("2025-07-31T07:27:57Z");
        String json = mapper.writeValueAsString(instant);
        assertEquals("\"2025-07-31T07:27:57Z\"", json);
    }

    @Test
    void testSerializeNullInstant() throws JsonProcessingException {
        ObjectMapper mapper = getMapper();
        Instant instant = null;
        String json = mapper.writeValueAsString(instant);
        assertEquals("null", json);
    }

    @Test
    void testDeserializeInstant() throws IOException {
        ObjectMapper mapper = getMapper();
        String json = "\"2025-07-31T07:27:57Z\"";
        Instant instant = mapper.readValue(json, Instant.class);
        assertEquals(Instant.parse("2025-07-31T07:27:57Z"), instant);
    }

    @Test
    void testDeserializeNullInstant() throws IOException {
        ObjectMapper mapper = getMapper();
        String json = "null";
        Instant instant = mapper.readValue(json, Instant.class);
        assertNull(instant);
    }

    @Test
    void testDeserializeInvalidInstant() {
        ObjectMapper mapper = getMapper();
        String json = "\"not-an-instant\"";
        assertThrows(Exception.class, () -> mapper.readValue(json, Instant.class));
    }
}
