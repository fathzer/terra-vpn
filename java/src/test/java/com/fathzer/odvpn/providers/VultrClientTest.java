package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

class VultrClientTest extends VPSProviderClientTestBase {
    private static final String API_URL = "https://api.vultr.com/v2/";
    private static final String INSTANCES_PATH = API_URL + "instances/";

    private static final String TEST_INSTANCE_ID = "480db6b7-2c94-4f3c-882a-1e2415e589f3";
    
    @Override
    protected Class<? extends BasicVPSProviderClient> getClientClass() {
        return VultrClient.class;
    }
    
    @Test
    void testGetState() throws Exception {
        final String uri = INSTANCES_PATH + TEST_INSTANCE_ID;
        // Ready
        String responseBody = "{\"instance\": {\"main_ip\": \"65.20.104.140\",\"power_status\": \"running\","+
            "\"server_status\": \"ok\",\"more\": \"just to check it will work if Vultr returns more data\"}}";
        setupMockResponse(uri, responseBody);
        assertEquals(new VPSState(TEST_INSTANCE_ID, "65.20.104.140", Status.READY), client.getState(TEST_INSTANCE_ID));
        // Response with empty instance object
        setupMockResponse(uri, "{\"instance\": null}");
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
        // Response with missing main_ip
        setupMockResponse(uri, "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + "\",\"power_status\":\"running\",\"server_status\":\"ok\"}}");
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
        // Response with missing power_status
        responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + "\",\"main_ip\":\"65.20.104.140\",\"server_status\":\"ok\"}}";
        setupMockResponse(uri, responseBody);
        assertEquals(new VPSState(TEST_INSTANCE_ID, "65.20.104.140", Status.IP_READY), client.getState(TEST_INSTANCE_ID));
        // Response with missing server_status
        responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + "\",\"main_ip\":\"65.20.104.140\",\"power_status\":\"running\"}}";
        setupMockResponse(uri, responseBody);
        assertEquals(new VPSState(TEST_INSTANCE_ID, "65.20.104.140", Status.IP_READY), client.getState(TEST_INSTANCE_ID));
        // Response with empty main_ip
        responseBody = "{\"instance\": {\"id\":\"" + TEST_INSTANCE_ID + "\",\"main_ip\":\" \",\"power_status\":\"running\",\"server_status\":\"ok\"}}";
        setupMockResponse(uri, responseBody);
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
    }
}
