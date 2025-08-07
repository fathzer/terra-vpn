package com.fathzer.odvpn.providers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import com.fathzer.odvpn.providers.utils.BasicVPSProviderClient;

class ScalewayClientTest extends VPSProviderClientTestBase {
    @Override
    protected void validateHeaders(java.net.http.HttpRequest request) {
        assertEquals(TEST_TOKEN, request.headers().firstValue("X-Auth-Token").orElse(""));
    }

    @Override
    protected Class<? extends BasicVPSProviderClient> getClientClass() {
        return ScalewayClient.class;
    }

    private ScalewayClient getScalewayClient() {
        return (ScalewayClient) client;
    }

    @Test
    void testSetProjectIdValidProjectSetsProjectId() throws Exception {
        String projectId = "test-project-id";
        String jsonResponse = """
        {
            "id":"test-project-id",
            "name":"Test Project",
            "organization_id":"org-123"
        }
        """;
        String uri = "https://api.scaleway.com/account/v3/projects/" + projectId;
        setupMockResponse(uri, jsonResponse);
        getScalewayClient().setProjectId(projectId);
        assertEquals(projectId, getScalewayClient().getProjectId(), "Project ID should be set when valid");
    }

    @Test
    void testSetProjectIdDefaultProjectSetsProjectIdNull() throws Exception {
        String projectId = "org-123"; // same as organization_id, triggers default logic
        String jsonResponse = """
        {
            "id":"org-123",
            "name":"Default Project",
            "organization_id":"org-123"
        }
        """;
        String uri = "https://api.scaleway.com/account/v3/projects/" + projectId;
        setupMockResponse(uri, jsonResponse);
        getScalewayClient().setProjectId(projectId);
        assertNull(getScalewayClient().getProjectId(), "Project ID should be null for default project");
    }
    
    @Test
    void testSetProjectIdUnknownProjectThrows() {
        String projectId = "unknown-id";
        String uri = "https://api.scaleway.com/account/v3/projects/" + projectId;
        String responseBody = """
        {
            "message": "resource is not found",
            "resource": "project_id",
            "resource_id": "unknown-id",
            "type": "not_found"
        }
        """;
        setupMockResponse(uri, "GET", 404, responseBody, null);
        ScalewayClient scalewayClient = getScalewayClient();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> scalewayClient.setProjectId(projectId));
        assertTrue(ex.getMessage().contains("Unknown project ID"));
    }

    @Test
    void testSetProjectIdMalformedProjectThrows() {
        String projectId = "bad-id";
        String uri = "https://api.scaleway.com/account/v3/projects/" + projectId;
        String responseBody = """
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
        ScalewayClient scalewayClient = getScalewayClient();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> scalewayClient.setProjectId(projectId));
        assertTrue(ex.getMessage().contains("Malformed project ID"));
    }
}
