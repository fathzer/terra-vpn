package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fathzer.http.Request;

class ScalewayClientTest extends VPSProviderClientTestBase<ScalewayClient> {
    private static final String KEYS_URI = "https://api.scaleway.com/iam/v1alpha1/ssh-keys";
    private static final String DEFAULT_SSH_KEYS_RESPONSE = """
        { "ssh_keys": [
            {"id":"k1","name":"mykey","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k2","name":"other","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k3","name":"disabled","organization_id":"org1","project_id":"org1","disabled":true},
            {"id":"k4","name":"false-disabled","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k5","name":"false-disabled","organization_id":"org1","project_id":"org1","disabled":true},
            {"id":"k6","name":"duplicated","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k7","name":"duplicated","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k8","name":"other-project","organization_id":"org1","project_id":"proj-other","disabled":false},
            {"id":"k9","name":"other-project-disabled","organization_id":"org1","project_id":"proj-other","disabled":true}
        ]}
        """;

    @Override
    protected void validateHeaders(Request request) {
        assertEquals(List.of(TEST_TOKEN), request.getHeaders().get("X-Auth-Token"));
    }

    @Override
    protected Class<ScalewayClient> getClientClass() {
        return ScalewayClient.class;
    }

    @Test
    void testSetProjectId() throws Exception {
        setupMockResponse(KEYS_URI, DEFAULT_SSH_KEYS_RESPONSE);

        // Test for existing project with no keys
        String projectId = "abc-123"; // same as organization_id, triggers default logic
        String uri = "https://api.scaleway.com/account/v3/projects/" + projectId;
        String jsonResponse;

        // Test for valid non default project => project should be set
        jsonResponse = """
        {
            "id":"abc-123",
            "name":"Test Project",
            "organization_id":"def-456"
        }
        """;
        setupMockResponse(uri, jsonResponse);
        client.setProjectId(projectId);
        assertEquals(projectId, client.getProjectId(), "Project ID should be set when valid");

        // Test for null or empty project ID => project should be default one
        client.setProjectId(" ");
        assertEquals("org1", client.getProjectId(), "Project ID should be default when set to empty string");
        client.setProjectId(null);
        assertEquals("org1", client.getProjectId(), "Project ID should be default when set to null");

        // Test for existing project with no ssh keys => project should be accepted
        jsonResponse = """
        {
            "id":"abc-123",
            "name":"Default Project",
            "organization_id":"abc-123"
        }
        """;
        setupMockResponse(uri, jsonResponse);
        client.setProjectId(projectId);
        assertEquals(projectId, client.getProjectId(), "Project ID should be set when valid");

        // Test for unknown project => should throw IllegalArgumentException
        String responseBody = """
        {
            "message": "resource is not found",
            "resource": "project_id",
            "resource_id": "abc-123",
            "type": "not_found"
        }
        """;
        setupMockResponse(uri, "GET", 404, responseBody, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> client.setProjectId(projectId));
        assertTrue(ex.getMessage().contains("Unknown project ID"));

        // Test for malformed project => should throw IllegalArgumentException
        responseBody = """
        {
            "details": [
                {
                    "argument_name": "project_id",
                    "help_message": "value must be a valid UUID",
                    "reason": "format"
                }
            ],
            "message": "invalid argument(s)",
            "type": "invalid_arguments"
        }""";
        setupMockResponse(uri, "GET", 400, responseBody, null);
        ex = assertThrows(IllegalArgumentException.class, () -> client.setProjectId(projectId));
        assertTrue(ex.getMessage().contains("Malformed project ID"));

        // Test with no ssh key and default project
        final String NoDefaultSshKeysResponse = """
        { "ssh_keys": [
            {"id":"k8","name":"other-project","organization_id":"org1","project_id":"proj-other","disabled":false}
        ]}
        """;
        setupMockResponse(KEYS_URI, NoDefaultSshKeysResponse);
        ex = assertThrows(IllegalArgumentException.class, () -> client.setProjectId(null));
        assertTrue(ex.getMessage().contains("Default project has no ssh keys"));
    }

    @Test
    void testGetSSHKeyId_allScenarios() throws Exception {
        // 1) Default project (projectId == null) -> match where organization_id == project_id
        String keysResponse = DEFAULT_SSH_KEYS_RESPONSE;
        setupMockResponse(KEYS_URI, keysResponse);
        client.setProjectId(null);
        assertEquals("k1", client.getSSHKeyId("mykey"));
        assertEquals("k4", client.getSSHKeyId("false-disabled"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("disabled"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("duplicated"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("other-project"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("unknown"));

        // 2) Specific project set -> only match keys with that project_id and name
        String projId = "proj-1";
        String projUri = "https://api.scaleway.com/account/v3/projects/" + projId;
        String projResponse = """
        {"id":"proj-1","name":"Some Project","organization_id":"org-xyz"}
        """;
        setupMockResponse(projUri, projResponse);
        keysResponse = keysResponse.replace("project_id\":\"org1", "project_id\":\""+projId);
        setupMockResponse(KEYS_URI, keysResponse);
        client.setProjectId(projId);
        assertEquals("k1", client.getSSHKeyId("mykey"));
        assertEquals("k4", client.getSSHKeyId("false-disabled"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("disabled"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("duplicated"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("other-project"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("unknown"));
    }
    @Test
    void testCheckInstanceType_valid() {
        String badRegion = "xx-bad-1";
        // No HTTP mock needed; should fail on region validation before calling the API
        assertThrows(IllegalArgumentException.class, () -> client.checkInstanceType(badRegion, "DEV1-S"));

        String region = "fr-par-1";
        String uri = "https://api.scaleway.com/instance/v1/zones/" + region + "/products/servers/availability?per_page=100";
        String responseBody = """
        { "servers": { "DEV1-S": { } } }
        """;
        setupMockResponse(uri, responseBody);
        assertDoesNotThrow(() -> client.checkInstanceType(region, "DEV1-S"));
        assertThrows(IllegalArgumentException.class, () -> client.checkInstanceType(region, "UNKNOWN-TYPE"));
    }

    @Test
    void testCreate() throws Exception {
        ObjectMapper om = new ObjectMapper();
        // Default project (projectId == null): project fields must be omitted
        setupMockResponse(KEYS_URI, DEFAULT_SSH_KEYS_RESPONSE);
        client.setProjectId(null);

        String region = "fr-par-1";
        String ipsUri = "https://api.scaleway.com/instance/v1/zones/" + region + "/ips/";
        String serversUri = "https://api.scaleway.com/instance/v1/zones/" + region + "/servers";

        // 1) Default project: IP creation should not include projectId
        setupMockResponse(ipsUri, "POST",
            """
            {"ip":{"id":"ip-1"}}
            """,
            body -> {
                try {
                    JsonNode root = om.readTree(body);
                    assertEquals("routed_ipv6", root.path("type").asText());
                    assertEquals("org1", root.path("project").asText(), "bad projectId for default project in IP creation request");
                } catch (Exception e) { fail(e); }
            }
        );
        setupMockResponse(serversUri, "POST",
            """
            {"server":{"id":"srv-1"}}
            """,
            body -> {
                try {
                    System.out.println(body);
                    JsonNode root = om.readTree(body);
                    assertEquals("org1", root.path("project").asText(), "bad projectId for default project in VPS creation request");
                    assertEquals("vm1", root.path("name").asText());
                    assertEquals("DEV1-S", root.path("commercial_type").asText());
                    assertEquals("41cce026-c90b-40cd-aead-a075a07196fb", root.path("image").asText());
                    assertFalse(root.path("dynamic_ip_required").asBoolean());
                    assertTrue(root.path("routed_ip_enabled").asBoolean());
                    assertEquals(1, root.path("ip_ids").size());
                    assertEquals("ip-1", root.path("ip_ids").get(0).asText());
                    assertEquals("l_ssd", root.path("volumes").get("0").get("volume_type").asText());
                    assertEquals("10000000000", root.path("volumes").get("0").get("size").asText());
                    assertEquals(1, root.path("tags").size());
                    assertEquals("On-Demand-Vpn", root.path("tags").get(0).asText());
                } catch (Exception e) { fail(e); }
            }
        );

        setupMockResponse(serversUri + "/srv-1/action", "POST", "{}", body -> {
            try {
                JsonNode root = om.readTree(body);
                assertEquals("poweron", root.path("action").asText());
            } catch (Exception e) { fail(e); }
        });

        String id = client.create(new com.fathzer.odvpn.providers.utils.VPSCreationSettings("vm1", region, "DEV1-S", "ignored"), null);
        assertEquals("srv-1/"+region+"/ip-1", id);

        // 2) Specific project: project fields must be present
        String projId = "proj-1";
        String projUri = "https://api.scaleway.com/account/v3/projects/" + projId;
        String projResponse = """
        {"id":"proj-1","name":"Some Project","organization_id":"org-xyz"}
        """;
        setupMockResponse(projUri, projResponse);
        client.setProjectId(projId);

        setupMockResponse(ipsUri, "POST",
            """
            {"ip":{"id":"ip-2"}}
            """,
            body -> {
                try {
                    JsonNode root = om.readTree(body);
                    assertEquals("routed_ipv6", root.path("type").asText());
                    assertEquals(projId, root.path("project").asText());
                } catch (Exception e) { fail(e); }
            }
        );
        setupMockResponse(serversUri, "POST",
            """
            {"server":{"id":"srv-2"}}
            """,
            body -> {
                try {
                    JsonNode root = om.readTree(body);
                    assertEquals(projId, root.path("project").asText());
                    assertEquals(1, root.path("ip_ids").size());
                    assertEquals("ip-2", root.path("ip_ids").get(0).asText());
                } catch (Exception e) { fail(e); }
            }
        );
        setupMockResponse(serversUri + "/srv-2/action", "POST", "{}", null);
        id = client.create(new com.fathzer.odvpn.providers.utils.VPSCreationSettings("vm2", region, "DEV1-S", "ignored"), null);
        assertEquals("srv-2/"+region+"/ip-2", id);
    }

    @Test
    void testDelete_callsServerAndIpDelete() {
        String serverId = "srv-123";
        String region = "fr-par-1";
        String ipId = "ip-789";
        String composedId = serverId + "/" + region + "/" + ipId;

        // Expect DELETE on instances path (super.delete)
        String deleteInstanceUri = "https://api.scaleway.com/instance/v1/zones/" + region + "/servers/" + serverId;
        setupMockResponse(deleteInstanceUri, "DELETE", "{}", null);

        // Expect DELETE on region IP endpoint
        String deleteIpUri = "https://api.scaleway.com/instance/v1/zones/" + region + "/ips/" + ipId;
        setupMockResponse(deleteIpUri, "DELETE", "{}", null);

        assertDoesNotThrow(() -> client.delete(composedId));
    }
}
