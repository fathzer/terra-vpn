package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

class DigitalOceanClientTest extends VPSProviderClientTestBase {
    private static final String API_URL = "https://api.digitalocean.com/v2/";
//    private static final String SERVERS_PATH = API_URL + "servers/";
    
    private static final String TEST_INSTANCE_ID = "103147789";
    private static final String IP = "65.20.104.140";

    // private static final String RESPONSE_BODY_FORMAT = """
    // 		{"server": {"id": "103147789","status":"%s","public_net": {"ipv4":%s}}}
    // """;
    // private static final String IP_V4_JSON = "{\"id\":12,\"ip\": \""+IP+"\"}}";

    @Override
    protected Class<? extends BasicVPSProviderClient> getClientClass() {
        return DigitalOceanClient.class;
    }

    @Test
    void testGetSSHKeyId() throws IOException {
        final String uri = API_URL + "account/keys";
        setupMockResponse(uri, "{\"ssh_keys\": [{\"id\": 48594634, \"name\": \"test-key\",\"extra\": 2}],\"extra\": 2}");
        assertEquals("48594634", client.getSSHKeyId("test-key"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("nonexistent-key"));
    }

    @Test
    void testGetRegions() {
        final String uri = API_URL + "regions?per_page=200";
        setupMockResponse(uri, """
        {
            "regions": [
                {"slug": "nyc1", "available": true, "sizes": ["s-1vcpu-1gb"], "extra":2}, 
                {"slug": "nyc2", "available": false, "sizes": ["s-1vcpu-1gb"], "extra":2}
            ]
        }
        """
        );
        assertDoesNotThrow(() -> client.checkRegion("nyc1"));
        assertThrows(IllegalArgumentException.class, () -> client.checkRegion("nonexistent-region"));
        assertThrows(IllegalArgumentException.class, () -> client.checkRegion("nyc2"));
    }

    // @Test
    // void testGetState() throws Exception {
    //     final String uri = SERVERS_PATH + TEST_INSTANCE_ID;
    //     // STARTING when no server object
    //     setupMockResponse(uri, "{\"server\": null}");
    //     assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
    //     // STARTING when null ipv4
    //     setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "starting", null));
    //     assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
    //     // IP_READY when ipv4 is provided
    //     setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "starting", IP_V4_JSON));
    //     assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.IP_READY), client.getState(TEST_INSTANCE_ID));
    //     // READY
    //     setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "running", IP_V4_JSON));
    //     assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.READY), client.getState(TEST_INSTANCE_ID));
    // }
}
