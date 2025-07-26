package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.jayway.jsonpath.JsonPath;
import java.util.List;

class VultrClientTest extends VPSProviderClientTestBase {
    private static final String API_URL = "https://api.vultr.com/v2/";
    private static final String INSTANCES_PATH = API_URL + "instances/";

    private static final String TEST_INSTANCE_ID = "480db6b7-2c94-4f3c-882a-1e2415e589f3";
    
    @Override
    protected Class<? extends BasicVPSProviderClient> getClientClass() {
        return VultrClient.class;
    }

    @Test
    void testCreate() throws Exception {
        var settings = new com.fathzer.odvpn.providers.utils.VPSCreationSettings("test-instance", "ewr", "vc2-1c-1gb", "98765");
        var vpnConfig = new com.fathzer.odvpn.repository.VPNConfig("vpn.example.com", null, com.fathzer.odvpn.repository.VPNConfig.Protocol.UDP, 1194);

        // Mock POST instance creation
        String instanceUri = API_URL + "instances";
        String instanceResponse = "{\"instance\": {\"id\": \"instance-id\"}}";
        setupMockResponse(instanceUri, "POST", instanceResponse, body -> {
            // Use JsonPath for robust, order-independent assertions
            // Requires Jayway JsonPath in test dependencies

            // Parse and check top-level fields
            assertEquals("ewr", JsonPath.read(body, "$.region"), "Region field incorrect");
            assertEquals("vc2-1c-1gb", JsonPath.read(body, "$.plan"), "Plan field incorrect");
            assertEquals("test-instance", JsonPath.read(body, "$.label"), "Label field incorrect");
            assertEquals("docker-ce", JsonPath.read(body, "$.image_id"), "Image ID field incorrect");
            List<String> sshKeys = JsonPath.read(body, "$.sshkey_id[*]");
            assertTrue(sshKeys.contains("98765"), "SSH key ID is missing from JSON");
            List<String> tags = JsonPath.read(body, "$.tags[*]");
            assertTrue(tags.contains("On-Demand-Vpn"), "Tags are missing from JSON");


        });

        String result = client.create(settings, vpnConfig);
        assertEquals("instance-id", result, "Returned instance ID does not match expected");
    }

    @Test
    void testDelete() {
        String id = "instance-id";
        String instanceUri = API_URL + "instances/" + id;
        setupMockResponse(instanceUri, "DELETE", "{}", null);
        assertDoesNotThrow(() -> client.delete(id), "Delete should not throw for valid instance ID");
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
