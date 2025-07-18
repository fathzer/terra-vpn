package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

class HetznerClientTest extends VPSProviderClientTestBase {
    private static final String API_URL = "https://api.hetzner.cloud/v1/";
    private static final String SERVERS_PATH = API_URL + "servers/";
    
    private static final String TEST_INSTANCE_ID = "103147789";
    private static final String IP = "65.20.104.140";

    private static final String RESPONSE_BODY_FORMAT = """
    		{"server": {"id": "103147789","status":"%s","public_net": {"ipv4":%s}}}
    """;
    private static final String IP_V4_JSON = "{\"id\":12,\"ip\": \""+IP+"\"}}";

    @Override
    protected Class<? extends BasicVPSProviderClient> getClientClass() {
        return HetznerClient.class;
    }

    @Test
    void testGetState() throws Exception {
        final String uri = SERVERS_PATH + TEST_INSTANCE_ID;
        // STARTING when no server object
        setupMockResponse(uri, "{\"server\": null}");
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
        // STARTING when null ipv4
        setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "starting", null));
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
        // IP_READY when ipv4 is provided
        setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "starting", IP_V4_JSON));
        assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.IP_READY), client.getState(TEST_INSTANCE_ID));
        // READY
        setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "running", IP_V4_JSON));
        assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.READY), client.getState(TEST_INSTANCE_ID));
    }
}
