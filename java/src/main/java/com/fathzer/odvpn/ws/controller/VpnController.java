package com.fathzer.odvpn.ws.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fathzer.odvpn.AbstractOnDemandVPNManager.DetailedStatus;
import com.fathzer.odvpn.OpenVPNManager.User;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.ws.Vpn;
import com.fathzer.odvpn.ws.VpnService;
import com.fathzer.odvpn.ws.VpnService.VpnException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/vpns")
public class VpnController {
	private static final String ILLEGAL_PARAM = "An illegal parameter was sent";
	private static final String CONFLICT = "Conflict, an locking operation is on going";
    private static final String VPN_NOT_FOUND = "VPN not found";
    
    private final VpnService service;

    public VpnController(VpnService service) {
        this.service = service;
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Creates or updates a VPN configuration", 
               description = "Creates or updates a VPN configuration without starting it. Optionally accepts a tar.gz file containing additional configuration.",
               tags = {"01 - vpns"},
               responses = {
                       @ApiResponse(responseCode = "201", description = "VPN is created"),
                       @ApiResponse(responseCode = "200", description = "VPN is updated"),
                       @ApiResponse(responseCode = "400", description = ILLEGAL_PARAM, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                       @ApiResponse(responseCode = "409", description = "VPN is running", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                       @ApiResponse(responseCode = "425", description = CONFLICT, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
                   })
    public ResponseEntity<Void> createVpn(
            @PathVariable String id,
            @RequestPart("config")
            @Parameter(description = "VPN configuration", 
                      content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = InstanceParameters.class)))
            InstanceParameters vpnDto,
            @RequestPart(value = "file", required = false)
            @Parameter(description = "Optional tar.gz file containing additional configuration")
            MultipartFile file) throws IOException {
        final boolean exists = service.exists(id);
        if (file == null || file.isEmpty()) {
            service.create(id, vpnDto, null, true);
        } else {
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();
            boolean isValidContentType = "application/gzip".equals(contentType) || "application/x-gzip".equals(contentType);
            boolean isValidExtension = originalFilename != null && originalFilename.toLowerCase().endsWith(".tar.gz");
            
            if (!isValidContentType && !isValidExtension) {
                throw new VpnException(HttpStatus.BAD_REQUEST, "Uploaded file must be a tar.gz file");
            }
            service.create(id, vpnDto, file.getInputStream(), true);
        }
        return exists ? ResponseEntity.ok().build() : ResponseEntity.created(linkTo(methodOn(VpnController.class).getVpn(id)).toUri()).build();
    }

    private record VPNs(List<String> vpns) {}

    @GetMapping
    @Operation(summary = "Lists all VPN configurations", 
               description = "Lists all VPN configurations",
               tags = {"01 - vpns"},
               responses = {
                       @ApiResponse(responseCode = "200", description = "Returns the list of VPN configurations", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
                       @ApiResponse(responseCode = "425", description = CONFLICT, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
                   })
    public VPNs getAllVpns() {
        return new VPNs(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieves a VPN configuration", 
               description = "Retrieves a VPN configuration by its ID",
               tags = {"01 - vpns"},
               responses = {
                       @ApiResponse(responseCode = "200", description = "Returns the VPN configuration", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
                       @ApiResponse(responseCode = "404", description = VPN_NOT_FOUND, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                       @ApiResponse(responseCode = "425", description = CONFLICT, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
                   })
    public Vpn getVpn(@PathVariable String id) {
        return service.findVpnById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes a VPN configuration", 
               description = "Deletes a VPN configuration by its ID",
               tags = {"01 - vpns"},
               responses = {
                       @ApiResponse(responseCode = "204", description = "VPN deleted"),
                       @ApiResponse(responseCode = "404", description = VPN_NOT_FOUND, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                       @ApiResponse(responseCode = "425", description = CONFLICT, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
                   })
    public Void deleteVpn(@PathVariable String id) throws IOException {
        service.delete(id, false);
        return null;
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Retrieves the status of a VPN", 
               description = "Retrieves the status of a VPN by its ID",
               tags = {"01 - vpns"},
               responses = {
                       @ApiResponse(responseCode = "200", description = "Returns the status of the VPN", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
                       @ApiResponse(responseCode = "404", description = VPN_NOT_FOUND, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                       @ApiResponse(responseCode = "425", description = CONFLICT, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
                   })
    public DetailedStatus getStatus(@PathVariable String id) throws IOException {
        return service.getStatus(id);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Starts a VPN", 
               description = "Starts a VPN by its ID. If the VPN is already started, it's refresh with its current configuration.",
               tags = {"02 - start/stop"},
               responses = {
                       @ApiResponse(responseCode = "202", description = "VPN started"),
                       @ApiResponse(responseCode = "404", description = VPN_NOT_FOUND, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                       @ApiResponse(responseCode = "425", description = CONFLICT, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
                   })
    public ResponseEntity<Void> startVpn(@PathVariable String id) {
        service.start(id);
        return ResponseEntity.accepted().header(HttpHeaders.LOCATION, "/vpns/" + id + "/status").build();
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stops a VPN", 
               description = "Stops a VPN by its ID",
               tags = {"02 - start/stop"},
               responses = {
                       @ApiResponse(responseCode = "204", description = "VPN stopped"),
                       @ApiResponse(responseCode = "404", description = "VPN not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                       @ApiResponse(responseCode = "409", description = "VPN is not running", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                       @ApiResponse(responseCode = "425", description = CONFLICT, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
                   })
    public Void stopVpn(@PathVariable String id) throws IOException {
        service.stop(id);
        return null;
    }

    private record Users(List<User> users) {}
    
    @GetMapping("/{id}/users")
    @Operation(summary = "List all VPN users", 
               description = "List all VPN users",
               tags = {"03 - users"},
               responses = {
                       @ApiResponse(responseCode = "200", description = "Returns the list of VPN users", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
                       @ApiResponse(responseCode = "404", description = VPN_NOT_FOUND, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                       @ApiResponse(responseCode = "425", description = CONFLICT, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
                   })
    public Users listVpnUsers(@PathVariable String id) throws IOException {
        return new Users(service.listUsers(id));
    }

    @PostMapping("/{id}/users/{user}")
    @Operation(summary = "Create a VPN user", 
               description = "Create a VPN user by its ID and name",
               tags = {"03 - users"},
               responses = {
                   @ApiResponse(responseCode = "201", description = "User created"),
                   @ApiResponse(responseCode = "404", description = "VPN not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
                   @ApiResponse(responseCode = "409", description = "User already exists", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
               })
    public ResponseEntity<Void> createVpnUser(@PathVariable String id, @PathVariable String user) throws IOException {
        service.createUser(id, user);
        return ResponseEntity.created(linkTo(methodOn(VpnController.class).getVpn(id)).toUri()).build();
    }

    @GetMapping(path = "/{id}/users/{user}/configuration")
    @Operation(summary = "Retrieves a VPN user configuration file", 
               description = "Retrieves a VPN user configuration file by its ID and name",
               tags = {"03 - users"},
               responses = {
                   @ApiResponse(responseCode = "200", description = "Returns the configuration file", 
                                content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
                   @ApiResponse(responseCode = "404", description = "VPN or user not found", 
                                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class)))
               })
    public ResponseEntity<byte[]> getVpnUserConfig(@PathVariable String id, @PathVariable String user) throws IOException {
        final byte[] config = service.getUserConfig(id, user);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + id + "-" + user + ".ovpn")
                .body(config);
    }

    @DeleteMapping("/{id}/users/{user}")
    @Operation(summary = "Revokes a VPN user",
               description = "Revokes a VPN user by its ID and name.",
               tags = {"03 - users"},
               responses = {
                @ApiResponse(responseCode = "204", description = "User revoked"),
                @ApiResponse(responseCode = "404", description = "VPN or user not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorObject.class))),
            })
    public Void deleteVpnUser(@PathVariable String id, @PathVariable String user) throws IOException {
        service.deleteUser(id, user);
        return null;
    }
}
