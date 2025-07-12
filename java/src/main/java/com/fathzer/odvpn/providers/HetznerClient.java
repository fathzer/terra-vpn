package com.fathzer.odvpn.providers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fathzer.odvpn.AbstractVPSProviderClient;
import com.fathzer.odvpn.VPSProvider.Status;
import com.fathzer.odvpn.VPSProvider.VPSState;

class HetznerClient extends AbstractVPSProviderClient {
    private static final String API_URL = "https://api.hetzner.cloud/v1";
    private final String token;

    HetznerClient(String token) {
        super();
        this.token = token;
    }

    @Override
    protected Builder newRequest(URI uri) {
        return super.newRequest(uri).header("Authorization", "Bearer " + this.token);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorResponse(@JsonProperty("error") String error,
        @JsonProperty("status") int status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SshKey(@JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("ssh_key") String key) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SshKeysResponse(@JsonProperty("ssh_keys") List<SshKey> sshKeys) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Region(@JsonProperty("id") String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RegionsResponse(@JsonProperty("regions") List<Region> regions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Plan(@JsonProperty("id") String id, 
        @JsonProperty("locations") List<String> locations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlansResponse(@JsonProperty("plans") List<Plan> plans) {}

    record InstanceCreationRequest(@JsonProperty("region") String region,
        @JsonProperty("plan") String plan,
        @JsonProperty("label") String label,
        @JsonProperty("image_id") String imageId,
        @JsonProperty("backups") String backups,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("sshkey_id") List<String> sshkeyIds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InstanceFullResponse(@JsonProperty("instance") InstanceResponse instance) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InstanceResponse(@JsonProperty("id") String id,
        @JsonProperty("main_ip") String mainIp,
        @JsonProperty("power_status") String powerStatus,
        @JsonProperty("server_status") String serverStatus) {

        public String mainIp() {
            if (mainIp != null && !mainIp.trim().isEmpty()) {
                return mainIp.trim();
            } else {
                return null;
            }
        }
    }

    /**
     * Checks if an SSH key with the given name exists in the Hetzner account.
     * @param keyName The name of the SSH key to check
     * @return The id of the SSH key if the key exists exactly once
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the key is unknown or duplicated
     */
    String getSSHKeyId(String keyName) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/ssh-keys")).build());
        final SshKeysResponse keysResponse = this.objectMapper.readValue(response.body(), SshKeysResponse.class);
        // Filter keys by name (case-sensitive)
        final List<SshKey> matchingKeys = keysResponse.sshKeys.stream()
            .filter(key -> keyName.equals(key.name))
            .toList();
        if (matchingKeys.isEmpty()) {
            throw new IllegalArgumentException("Unknown key");
        } else if (matchingKeys.size() > 1) {
            throw new IllegalArgumentException("Duplicated key");
        }
        return matchingKeys.get(0).id;
    }

    void checkZone(String zone) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/regions")).build());
        final RegionsResponse regionsResponse = this.objectMapper.readValue(response.body(), RegionsResponse.class);
        if (regionsResponse.regions().stream().map(Region::id).noneMatch(zone::equals)) {
            throw new IllegalArgumentException("Unknown zone " + zone);
        }
    }

    void checkInstanceType(String zone, String instanceType) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/plans")).build());
        final PlansResponse plansResponse = this.objectMapper.readValue(response.body(), PlansResponse.class);
        boolean exists = plansResponse.plans.stream()
            .anyMatch(plan -> plan.id.equals(instanceType) && plan.locations.contains(zone));
        if (!exists) {
            throw new IllegalArgumentException("Unknown instance type " + instanceType + " for zone " + zone);
        }
    }

    private String getErrorMessage(HttpResponse<String> response) {
        try {
            final ErrorResponse errorResponse = this.objectMapper.readValue(response.body(), ErrorResponse.class);
            return errorResponse.error;
        } catch (IOException e) {
            return "Unknown error with " + response.statusCode()+ " status code";
        }
    }

    @Override
    protected AuthenticationException getAuthenticationException(HttpResponse<String> response) throws IOException {
        return new AuthenticationException(response.statusCode(), this.getErrorMessage(response));
    }

    @Override
    protected ErrorResponseException getErrorResponseException(HttpResponse<String> response) throws IOException {
        return new ErrorResponseException(response.statusCode(), this.getErrorMessage(response));
    }

    @Override
    protected ServerErrorException getServerErrorException(HttpResponse<String> response) throws IOException {
        return new ServerErrorException(response.statusCode(), this.getErrorMessage(response));
    }

    String create(InstanceCreationRequest request) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/instances")).POST(HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(request))).build());
        final InstanceResponse instanceResponse = this.objectMapper.readValue(response.body(), InstanceFullResponse.class).instance();
        return instanceResponse.id;
    }

    VPSState getState(String id) throws IOException {
        final HttpResponse<String> response = this.doRequest(this.newRequest(URI.create(API_URL + "/instances/" + id)).build());
        final InstanceResponse instanceResponse = this.objectMapper.readValue(response.body(), InstanceFullResponse.class).instance();
        final Status status;
        if (instanceResponse==null || instanceResponse.mainIp()==null) {
            status = Status.STARTING;
        } else if ("running".equals(instanceResponse.powerStatus()) && "ok".equals(instanceResponse.serverStatus())) {
            status = Status.READY;
        } else {
            status = Status.IP_READY;
        }
        return new VPSState(id, instanceResponse==null ? null : instanceResponse.mainIp(), status);
    }

    void delete(String id) throws IOException {
        this.doRequest(this.newRequest(URI.create(API_URL + "/instances/" + id)).DELETE().build());
    }
}

/** Creates a new instance reply
 {
    "server": {
        "id": 103147789,
        "name": "test-server",
        "status": "initializing",
        "server_type": {
            "id": 22,
            "name": "cpx11",
            "architecture": "x86",
            "cores": 2,
            "cpu_type": "shared",
            "deprecated": false,
            "deprecation": null,
            "description": "CPX 11",
            "disk": 40,
            "memory": 2,
            "prices": [
                {
                    "location": "ash",
                    "price_hourly": {
                        "gross": "0.0086400000000000",
                        "net": "0.0072000000"
                    },
                    "price_monthly": {
                        "gross": "5.3880000000000000",
                        "net": "4.4900000000"
                    },
                    "included_traffic": 1099511627776,
                    "price_per_tb_traffic": {
                        "gross": "1.2000000000000000",
                        "net": "1.0000000000"
                    }
                },
                {
                    "location": "fsn1",
                    "price_hourly": {
                        "gross": "0.0075600000000000",
                        "net": "0.0063000000"
                    },
                    "price_monthly": {
                        "gross": "4.6200000000000000",
                        "net": "3.8500000000"
                    },
                    "included_traffic": 21990232555520,
                    "price_per_tb_traffic": {
                        "gross": "1.2000000000000000",
                        "net": "1.0000000000"
                    }
                },
                {
                    "location": "hel1",
                    "price_hourly": {
                        "gross": "0.0075600000000000",
                        "net": "0.0063000000"
                    },
                    "price_monthly": {
                        "gross": "4.6200000000000000",
                        "net": "3.8500000000"
                    },
                    "included_traffic": 21990232555520,
                    "price_per_tb_traffic": {
                        "gross": "1.2000000000000000",
                        "net": "1.0000000000"
                    }
                },
                {
                    "location": "hil",
                    "price_hourly": {
                        "gross": "0.0086400000000000",
                        "net": "0.0072000000"
                    },
                    "price_monthly": {
                        "gross": "5.3880000000000000",
                        "net": "4.4900000000"
                    },
                    "included_traffic": 1099511627776,
                    "price_per_tb_traffic": {
                        "gross": "1.2000000000000000",
                        "net": "1.0000000000"
                    }
                },
                {
                    "location": "nbg1",
                    "price_hourly": {
                        "gross": "0.0075600000000000",
                        "net": "0.0063000000"
                    },
                    "price_monthly": {
                        "gross": "4.6200000000000000",
                        "net": "3.8500000000"
                    },
                    "included_traffic": 21990232555520,
                    "price_per_tb_traffic": {
                        "gross": "1.2000000000000000",
                        "net": "1.0000000000"
                    }
                },
                {
                    "location": "sin",
                    "price_hourly": {
                        "gross": "0.0142800000000000",
                        "net": "0.0119000000"
                    },
                    "price_monthly": {
                        "gross": "8.8800000000000000",
                        "net": "7.4000000000"
                    },
                    "included_traffic": 1099511627776,
                    "price_per_tb_traffic": {
                        "gross": "8.8800000000000000",
                        "net": "7.4000000000"
                    }
                }
            ],
            "storage_type": "local"
        },
        "datacenter": {
            "id": 2,
            "description": "Nuremberg 1 virtual DC 3",
            "location": {
                "id": 2,
                "name": "nbg1",
                "description": "Nuremberg DC Park 1",
                "city": "Nuremberg",
                "country": "DE",
                "latitude": 49.452102,
                "longitude": 11.076665,
                "network_zone": "eu-central"
            },
            "name": "nbg1-dc3",
            "server_types": {
                "available": [
                    22,
                    23,
                    24,
                    27,
                    28,
                    29,
                    30,
                    31,
                    32,
                    45,
                    96,
                    97,
                    98,
                    99,
                    100,
                    101,
                    104
                ],
                "available_for_migration": [
                    22,
                    23,
                    24,
                    27,
                    28,
                    29,
                    30,
                    31,
                    32,
                    45,
                    96,
                    97,
                    98,
                    99,
                    100,
                    101,
                    104
                ],
                "supported": [
                    1,
                    3,
                    5,
                    7,
                    9,
                    11,
                    12,
                    13,
                    14,
                    15,
                    22,
                    23,
                    24,
                    25,
                    26,
                    33,
                    34,
                    35,
                    36,
                    37,
                    38,
                    39,
                    40,
                    41,
                    42,
                    43,
                    44,
                    45,
                    93,
                    94,
                    95,
                    96,
                    97,
                    98,
                    99,
                    100,
                    101,
                    104,
                    105,
                    106,
                    107
                ]
            }
        },
        "image": {
            "id": 40093247,
            "type": "app",
            "name": "docker-ce",
            "architecture": "x86",
            "bound_to": null,
            "created_from": null,
            "deprecated": null,
            "description": "docker-ce",
            "disk_size": 40,
            "image_size": null,
            "labels": {},
            "os_flavor": "ubuntu",
            "os_version": "unknown",
            "protection": {
                "delete": false
            },
            "rapid_deploy": true,
            "status": "available",
            "created": "2021-06-08T06:22:47Z",
            "deleted": null
        },
        "iso": null,
        "primary_disk_size": 40,
        "labels": {
            "environment": "test",
            "example.com/my": "label",
            "just-a-key": ""
        },
        "protection": {
            "delete": false,
            "rebuild": false
        },
        "backup_window": null,
        "rescue_enabled": false,
        "locked": false,
        "placement_group": null,
        "public_net": {
            "firewalls": [],
            "floating_ips": [],
            "ipv4": {
                "id": 94673092,
                "ip": "116.203.77.243",
                "blocked": false,
                "dns_ptr": "static.243.77.203.116.clients.your-server.de"
            },
            "ipv6": {
                "id": 94673093,
                "ip": "2a01:4f8:1c1c:47eb::/64",
                "blocked": false,
                "dns_ptr": []
            }
        },
        "private_net": [],
        "load_balancers": [],
        "volumes": [],
        "included_traffic": 0,
        "ingoing_traffic": 0,
        "outgoing_traffic": 0,
        "created": "2025-07-10T12:41:52Z"
    },
    "root_password": null,
    "action": {
        "id": 566420293308478,
        "command": "create_server",
        "started": "2025-07-10T12:41:52Z",
        "finished": null,
        "progress": 0,
        "status": "running",
        "resources": [
            {
                "id": 103147789,
                "type": "server"
            },
            {
                "id": 40093247,
                "type": "image"
            }
        ],
        "error": null
    },
    "next_actions": [
        {
            "id": 566420293308479,
            "command": "start_server",
            "started": "2025-07-10T12:41:52Z",
            "finished": null,
            "progress": 0,
            "status": "running",
            "resources": [
                {
                    "id": 103147789,
                    "type": "server"
                }
            ],
            "parent_id": 566420293308478,
            "error": null
        }
    ]
}
 */