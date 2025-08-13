package com.fathzer.odvpn.providers.utils;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

class BasicVPSSettingsTest {
    @Test
    void testDefaultValues() {
        BasicVPSSettings settings = new BasicVPSSettings();
        assertNull(settings.getRegion());
        assertNull(settings.getInstanceType());
        assertEquals("odvpn", settings.getSshKeyName());
        assertEquals("root", settings.getSshUser());
    }

    @Test
    void testSettersAndGetters() {
        BasicVPSSettings settings = new BasicVPSSettings();
        settings.setRegion("us-east-1");
        settings.setInstanceType("t2.micro");
        settings.setSshKeyName("customKey");
        settings.setSshUser("admin");

        assertEquals("us-east-1", settings.getRegion());
        assertEquals("t2.micro", settings.getInstanceType());
        assertEquals("customKey", settings.getSshKeyName());
        assertEquals("admin", settings.getSshUser());
    }

    @Test
    void testGetRegionWithDefault() {
        BasicVPSSettings settings = new BasicVPSSettings();
        assertEquals("default-region", settings.getRegion("default-region"));
        settings.setRegion("eu-west-1");
        assertEquals("eu-west-1", settings.getRegion("default-region"));
    }

    @Test
    void testGetInstanceTypeWithDefault() {
        BasicVPSSettings settings = new BasicVPSSettings();
        assertEquals("default-type", settings.getInstanceType("default-type"));
        settings.setInstanceType("c5.large");
        assertEquals("c5.large", settings.getInstanceType("default-type"));
    }

    @Test
    void testSetNullValues() {
        BasicVPSSettings settings = new BasicVPSSettings();
        settings.setSshKeyName(null);
        settings.setSshUser(null);
        assertEquals("odvpn", settings.getSshKeyName());
        assertEquals("root", settings.getSshUser());
    }

    @Test
    void testConfigDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
        {
            "region": "us-east-1",
            "instanceType": "t2.micro",
            "sshKeyName": "customKey",
            "sshUser": "admin"
        }
        """;
        BasicVPSSettings settings = mapper.readValue(json, BasicVPSSettings.class);
        assertEquals("us-east-1", settings.getRegion());
        assertEquals("t2.micro", settings.getInstanceType());
        assertEquals("customKey", settings.getSshKeyName());
        assertEquals("admin", settings.getSshUser());
    }
}
