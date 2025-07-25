package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.providers.utils.VPSCreationSettings;

class DigitalOceanClientTest extends VPSProviderClientTestBase {
    private static final String API_URL = "https://api.digitalocean.com/v2/";
    private static final String SERVERS_PATH = API_URL + "droplets/";
    
    private static final String TEST_INSTANCE_ID = "103147789";
    private static final String IP = "65.20.104.140";

    private static final String RESPONSE_BODY_FORMAT = """
		{"droplet": {"id": "103147789","status":"%s","networks": {"v4":[%s]}}}
    """;
    private static final String IP_V4_JSON = "{\"ip_address\": \""+IP+"\"}";

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

    @Test
    void testCreate() throws Exception {
        var settings = new VPSCreationSettings("test-droplet", "nyc1", "s-1vcpu-1gb", "12345");
        var vpnConfig = new VPNConfig("vpn.example.com", null, VPNConfig.Protocol.UDP, 1194);

        // Mock POST droplet creation
        String dropletUri = API_URL + "droplets";
        String dropletResponse = "{\"droplet\": {\"id\": \"droplet-id\"}}";
        setupMockResponse(dropletUri, "POST", dropletResponse, body -> {
            assertNotNull(body, "Request body JSON of droplet request is null");
            assertTrue(body.contains("\"name\":\"test-droplet\""), "Droplet name is missing from JSON");
            assertTrue(body.contains("\"region\":\"nyc1\""), "Region is missing from JSON");
            assertTrue(body.contains("\"size\":\"s-1vcpu-1gb\""), "Size is missing from JSON");
            assertTrue(body.contains("\"ssh_keys\":[\"12345\"]"), "SSH key is missing from JSON");            
        });

        // Mock POST firewall creation
        String firewallUri = API_URL + "firewalls";
        String firewallResponse = "{\"firewall\": {\"id\": \"firewall-id\"}}";
        setupMockResponse(firewallUri, "POST", firewallResponse, body -> {
            assertNotNull(body, "Request body JSON of firewall request is null");
            assertTrue(body.contains("\"protocol\":\"udp\""), "UDP rule is missing from JSON");
            assertTrue(body.contains("\"ports\":\"1194\""), "UDP port 1194 is missing from JSON");
            assertTrue(body.contains("\"protocol\":\"tcp\""), "TCP rule is missing from JSON");
            assertTrue(body.contains("\"ports\":\"22\""), "TCP port 22 is missing from JSON");
        });

        // Call the method to test
        String result = client.create(settings, vpnConfig);
        assertEquals("droplet-id/firewall-id", result);
    }

    @Test
    void testDelete() {
        String id = "droplet-id/firewall-id";
        // Mock DELETE droplet
        String dropletUri = API_URL + "droplets/" + "droplet-id";
        setupMockResponse(dropletUri, "DELETE", "{}", null);
        // Mock DELETE firewall
        String firewallUri = API_URL + "firewalls/" + "firewall-id";
        setupMockResponse(firewallUri, "DELETE", "{}", null);

        assertDoesNotThrow(() -> client.delete(id));
    }

    @Test
    void testGetState() throws Exception {
        final String uri = SERVERS_PATH + TEST_INSTANCE_ID;
        // STARTING when no server object
        setupMockResponse(uri, "{\"droplet\": null}");
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
        // STARTING when null ipv4
        setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "new", null));
        assertEquals(new VPSState(TEST_INSTANCE_ID, null, Status.STARTING), client.getState(TEST_INSTANCE_ID));
        // IP_READY when ipv4 is provided
        setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "new", IP_V4_JSON));
        assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.IP_READY), client.getState(TEST_INSTANCE_ID));
        // READY
        setupMockResponse(uri, String.format(RESPONSE_BODY_FORMAT, "active", IP_V4_JSON));
        assertEquals(new VPSState(TEST_INSTANCE_ID, IP, Status.READY), client.getState(TEST_INSTANCE_ID));
    }
}

