package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.List;
import com.fathzer.odvpn.providers.utils.VPSCreationSettings;
import com.fathzer.odvpn.repository.VPNConfig;
import static org.mockito.Mockito.*;

class VultrClientTest extends VPSProviderClientTestBase<VultrClient> {
    // Expose protected getErrorMessage for testing
    public static class TestableVultrClient extends VultrClient {
        public TestableVultrClient() {
            super("dummy-token"); // Provide a dummy API token for testing
        }
        public String getErrorMessagePublic(HttpResponse<String> response) {
            return super.getErrorMessage(response);
        }
    }
    private static final String API_URL = "https://api.vultr.com/v2/";
    private static final String INSTANCES_PATH = API_URL + "instances/";

    private static final String TEST_INSTANCE_ID = "480db6b7-2c94-4f3c-882a-1e2415e589f3";
    
    @Override
    protected Class<VultrClient> getClientClass() {
        return VultrClient.class;
    }

    @Test
    void testCheckRegionAndInstanceType() {
        // /regions mock: ewr and fra1 exist, sgp does not
        String regionsUri = API_URL + "regions";
        setupMockResponse(regionsUri, """
        { "regions": [ {"id": "ewr"}, {"id": "fra1"} ] }
        """);
        assertDoesNotThrow(() -> client.checkRegion("ewr"));
        assertDoesNotThrow(() -> client.checkRegion("fra1"));
        assertThrows(IllegalArgumentException.class, () -> client.checkRegion("sgp"));

        // /plans mock: vc2-1c-1gb available in ewr, not in fra1; vc2-2c-2gb only in fra1
        String plansUri = API_URL + "plans";
        setupMockResponse(plansUri, """
        { "plans": [
            {"id": "vc2-1c-1gb", "locations": [ "ewr" ] },
            {"id": "vc2-2c-2gb", "locations": [ "fra1" ] }
        ] }
        """);
        // vc2-1c-1gb in ewr is valid
        assertDoesNotThrow(() -> client.checkInstanceType("ewr", "vc2-1c-1gb"));
        // vc2-2c-2gb in fra1 is valid
        assertDoesNotThrow(() -> client.checkInstanceType("fra1", "vc2-2c-2gb"));
        // vc2-1c-1gb in fra1 is invalid
        assertThrows(IllegalArgumentException.class, () -> client.checkInstanceType("fra1", "vc2-1c-1gb"));
        // vc2-2c-2gb in ewr is invalid
        assertThrows(IllegalArgumentException.class, () -> client.checkInstanceType("ewr", "vc2-2c-2gb"));
        // unknown plan
        assertThrows(IllegalArgumentException.class, () -> client.checkInstanceType("ewr", "doesnotexist"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetErrorMessage() {
        try (TestableVultrClient testClient = new TestableVultrClient()) {
            // Mockito mock for valid error JSON
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(400);
            when(response.body()).thenReturn("{\"error\":\"Some error\",\"status\":400}");
            assertEquals("Some error", testClient.getErrorMessagePublic(response));
            // Mockito mock for malformed JSON
            HttpResponse<String> badResponse = mock(HttpResponse.class);
            when(badResponse.statusCode()).thenReturn(500);
            when(badResponse.body()).thenReturn("not json");
            assertTrue(testClient.getErrorMessagePublic(badResponse).contains("Unknown error"));
            assertTrue(testClient.getErrorMessagePublic(badResponse).contains("500"));
        }
    }

    @Test
    void testCreate() throws Exception {
        var settings = new VPSCreationSettings("test-instance", "ewr", "vc2-1c-1gb", "98765");
        var vpnConfig = new VPNConfig("vpn.example.com", null, VPNConfig.Protocol.UDP, 1194);

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
