package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import java.net.http.HttpRequest;

import org.junit.jupiter.api.Test;

class ScalewayClientTest extends VPSProviderClientTestBase<ScalewayClient> {
    @Override
    protected void validateHeaders(HttpRequest request) {
        assertEquals(TEST_TOKEN, request.headers().firstValue("X-Auth-Token").orElse(""));
    }

    @Override
    protected Class<ScalewayClient> getClientClass() {
        return ScalewayClient.class;
    }

    @Test
    void testSetProjectId() throws Exception {
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

        // Test for null or empty project ID => project should be null
        client.setProjectId(" ");
        assertNull(client.getProjectId(), "Project ID should be null when set to empty string");
        // Reset project ID for next tests
        client.setProjectId(projectId);
        client.setProjectId(null);
        assertNull(client.getProjectId(), "Project ID should be null when set to null");
        // Reset project ID for next tests
        client.setProjectId(projectId);

        // Test for default project => project should be null
        jsonResponse = """
        {
            "id":"abc-123",
            "name":"Default Project",
            "organization_id":"abc-123"
        }
        """;
        setupMockResponse(uri, jsonResponse);
        client.setProjectId(projectId);
        assertNull(client.getProjectId(), "Project ID should be null for default project");

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
    void testGetSSHKeyId_allScenarios() throws Exception {
        final String keysUri = "https://api.scaleway.com/iam/v1alpha/ssh-keys";

        // 1) Default project (projectId == null) -> match where organization_id == project_id
        String keysResponse = """
        { "ssh_keys": [
            {"id":"k1","name":"mykey","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k2","name":"other","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k3","name":"disabled","organization_id":"org1","project_id":"org1","disabled":true},
            {"id":"k4","name":"false-disabled","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k5","name":"false-disabled","organization_id":"org1","project_id":"org1","disabled":true},
            {"id":"k6","name":"duplicated","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k7","name":"duplicated","organization_id":"org1","project_id":"org1","disabled":false},
            {"id":"k8","name":"other-project","organization_id":"org1","project_id":"proj-other","disabled":false}
        ]}
        """;
        setupMockResponse(keysUri, keysResponse);
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
        client.setProjectId(projId);
        keysResponse = keysResponse.replace("project_id\":\"org1", "project_id\":\""+projId);
        setupMockResponse(keysUri, keysResponse);
        assertEquals("k1", client.getSSHKeyId("mykey"));
        assertEquals("k4", client.getSSHKeyId("false-disabled"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("disabled"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("duplicated"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("other-project"));
        assertThrows(IllegalArgumentException.class, () -> client.getSSHKeyId("unknown"));
    }
}
