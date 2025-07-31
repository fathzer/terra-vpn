package com.fathzer.odvpn.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

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
    void testRoundTrip() throws JsonProcessingException {
        ObjectMapper mapper = getMapper();
        Instant instant = Instant.parse("2025-07-31T07:27:57Z");
        assertEquals(instant, mapper.readValue(mapper.writeValueAsString(instant), Instant.class));
    }

    @Test
    void testSerializeNullInstant() throws JsonProcessingException {
        ObjectMapper mapper = getMapper();
        assertEquals("null", mapper.writeValueAsString(null));
        assertNull(mapper.readValue("null", Instant.class));
    }

    @Test
    void testDeserializeInvalidInstant() {
        ObjectMapper mapper = getMapper();
        String json = "\"not-an-instant\"";
        assertThrows(Exception.class, () -> mapper.readValue(json, Instant.class));
    }
}
