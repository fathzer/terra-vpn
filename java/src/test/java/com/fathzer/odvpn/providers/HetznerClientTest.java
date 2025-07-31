package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.jayway.jsonpath.JsonPath;
import java.util.List;

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
    void testGetSSHKeyId() throws Exception {
        String sshKeysUri = API_URL + "ssh_keys";
        // Single key found
        setupMockResponse(sshKeysUri, "{\"ssh_keys\": [{\"id\": \"123\", \"name\": \"vpn-key\"}] }");
        assertEquals("123", client.getSSHKeyId("vpn-key"));
        // No key found
        setupMockResponse(sshKeysUri, "{\"ssh_keys\": [{\"id\": \"124\", \"name\": \"other-key\"}] }");
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("vpn-key"));
        // Duplicated keys
        setupMockResponse(sshKeysUri, "{\"ssh_keys\": [{\"id\": \"123\", \"name\": \"vpn-key\"}, {\"id\": \"124\", \"name\": \"vpn-key\"}] }");
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("vpn-key"));
    }

    @Test
    void testCheckRegionAndInstanceType() {
        // /locations mock: fsn1 and nbg1 exist
        String locationsUri = API_URL + "locations";
        setupMockResponse(locationsUri, """
        { "locations": [ {"name": "fsn1"}, {"name": "nbg1"} ] }
        """);
        assertDoesNotThrow(() -> client.checkRegion("fsn1"));
        assertDoesNotThrow(() -> client.checkRegion("nbg1"));
        assertThrows(IllegalArgumentException.class, () -> client.checkRegion("doesnotexist"));

        // /server_types mock: cx11 available in fsn1, not in nbg1; cx21 only in nbg1
        String typesUri = API_URL + "server_types";
        setupMockResponse(typesUri, """
        { "server_types": [
            {"name": "cx11", "prices": [ {"location": "fsn1"} ] },
            {"name": "cx21", "prices": [ {"location": "nbg1"} ] }
        ] }
        """);
        // cx11 in fsn1 is valid
        assertDoesNotThrow(() -> client.checkInstanceType("fsn1", "cx11"));
        // cx21 in nbg1 is valid
        assertDoesNotThrow(() -> client.checkInstanceType("nbg1", "cx21"));
        // cx11 in nbg1 is invalid
        assertThrows(IllegalArgumentException.class, () -> client.checkInstanceType("nbg1", "cx11"));
        // cx21 in fsn1 is invalid
        assertThrows(IllegalArgumentException.class, () -> client.checkInstanceType("fsn1", "cx21"));
        // unknown type
        assertThrows(IllegalArgumentException.class, () -> client.checkInstanceType("fsn1", "doesnotexist"));
    }

    @Test
    void testCreate() throws Exception {
        var settings = new com.fathzer.odvpn.providers.utils.VPSCreationSettings("test-server", "fsn1", "cx11", "12345");
        var vpnConfig = new com.fathzer.odvpn.repository.VPNConfig("vpn.example.com", null, com.fathzer.odvpn.repository.VPNConfig.Protocol.UDP, 1194);

        // Mock POST server creation
        String serverUri = API_URL + "servers";
        String serverResponse = "{\"server\": {\"id\": \"server-id\"}}";
        setupMockResponse(serverUri, "POST", serverResponse, body -> {
            assertEquals("test-server", JsonPath.read(body, "$.name"), "Server name is missing or incorrect");
            assertEquals("fsn1", JsonPath.read(body, "$.location"), "Location is missing or incorrect");
            assertEquals("cx11", JsonPath.read(body, "$.server_type"), "Server type is missing or incorrect");
            List<String> sshKeys = JsonPath.read(body, "$.ssh_keys[*]");
            assertTrue(sshKeys.contains("12345"), "SSH key is missing from JSON");
            // Check labels
            String label = JsonPath.read(body, "$.labels.application");
            assertEquals("On-Demand-VPN", label, "Labels are missing or incorrect");
        });

        String result = client.create(settings, vpnConfig);
        assertEquals("server-id", result, "Returned server ID does not match expected");
    }

    @Test
    void testDelete() {
        String id = "server-id";
        String serverUri = API_URL + "servers/" + id;
        setupMockResponse(serverUri, "DELETE", "{}", null);
        assertDoesNotThrow(() -> client.delete(id), "Delete should not throw for valid server ID");
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
        // STARTING when empty ipv4
        setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "starting", "{\"id\":12,\"ip\": \"\"}"));
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
        // IP_READY when ipv4 is provided
        setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "starting", IP_V4_JSON));
        assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.IP_READY), client.getState(TEST_INSTANCE_ID));
        // READY
        setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "running", IP_V4_JSON));
        assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.READY), client.getState(TEST_INSTANCE_ID));
    }
}
