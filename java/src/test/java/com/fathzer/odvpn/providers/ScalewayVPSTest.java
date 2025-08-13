package com.fathzer.odvpn.providers;


import static com.fathzer.odvpn.json.VPSProviderTestUtils.deserialize;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

import org.junit.jupiter.api.Test;

import java.io.IOException;


class ScalewayVPSTest {
    @Test
    void testName() {
        assertEquals("Scaleway VPS", new ScalewayVPS().name());
    }

    @Test
    void testConfigClass() {
        assertEquals(ScalewaySettings.class, new ScalewayVPS().getConfigClass());
    }

    @Test
    void testDefaultRegionAndType() {
        ScalewayVPS vps = new ScalewayVPS();
        assertEquals("pl-waw-2", vps.getDefaultRegion());
        assertEquals("STARDUST1-S", vps.getDefaultInstanceType());
    }

    @Test
    void testGetClient() {
        ScalewayVPS vps = new ScalewayVPS();
        vps.setSettings(new ScalewaySettings());
        BasicVPSProviderClient client = vps.getClient();
        assertNotNull(client);
    }

    @Test
    void testDeserialization() throws IOException {
    	ObjectMapper mapper = new ObjectMapper();
        String json = """
        {
            "token": "myToken",
            "projectId": "myProject",
            "region": "fr-par-1",
            "instanceType": "DEV1-S"
        }
        """;
        ScalewaySettings settings = mapper.readValue(json, ScalewaySettings.class);
        assertEquals("myToken", settings.getToken());
        assertEquals("myProject", settings.getProjectId());
        assertEquals("fr-par-1", settings.getRegion());
        assertEquals("DEV1-S", settings.getInstanceType());

        json = """
            {
            "providerId": "scaleway",
            "config": {
                "projectId": "myProject",
                "region": "fr-par-1",
                "instanceType": "DEV1-S"
                }
            }
            """;
        ScalewayVPS vps2 = deserialize(mapper, json);
        assertEquals("myProject", vps2.getSettings().getProjectId());
    }
}
