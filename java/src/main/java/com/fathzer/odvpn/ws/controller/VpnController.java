package com.fathzer.odvpn.ws.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fathzer.odvpn.AbstractOnDemandVPNManager.DetailedStatus;
import com.fathzer.odvpn.repository.InstanceParameters;
import com.fathzer.odvpn.ws.Vpn;
import com.fathzer.odvpn.ws.VpnService;
import com.fathzer.odvpn.ws.VpnService.VpnException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
    private final VpnService service;

    public VpnController(VpnService service) {
        this.service = service;
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Creates or updates a VPN configuration", 
               description = "Creates or updates a VPN configuration without starting it. Optionally accepts a tar.gz file containing additional configuration.",
               tags = {"01 - vpns"})
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
        Path tempFile;
        if (file == null || file.isEmpty()) {
            tempFile = null;
        } else {
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();
            boolean isValidContentType = "application/gzip".equals(contentType) || "application/x-gzip".equals(contentType);
            boolean isValidExtension = originalFilename != null && originalFilename.toLowerCase().endsWith(".tar.gz");
            
            if (!isValidContentType && !isValidExtension) {
                throw new VpnException(HttpStatus.BAD_REQUEST, "Uploaded file must be a tar.gz file");
            }
            tempFile = Files.createTempFile("openvpn", ".tar.gz");
            file.transferTo(tempFile);
        }
        try {
            service.create(id, vpnDto, tempFile, true);
        } finally {
            if (tempFile != null) {
                Files.delete(tempFile);
            }
        }
        return exists ? ResponseEntity.ok().build() : ResponseEntity.created(linkTo(methodOn(VpnController.class).getVpn(id)).toUri()).build();
    }

    @GetMapping
    @Operation(summary = "Lists all VPN configurations", 
               description = "Lists all VPN configurations",
               tags = {"01 - vpns"})
    public List<String> getAllVpns() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieves a specific VPN configuration", 
               description = "Retrieves a specific VPN configuration by its ID",
               tags = {"01 - vpns"})
    public Vpn getVpn(@PathVariable String id) {
        return service.findVpnById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes a specific VPN configuration", 
               description = "Deletes a specific VPN configuration by its ID",
               tags = {"01 - vpns"})
    public Void deleteVpn(@PathVariable String id) throws IOException {
        service.delete(id, false);
        return null;
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Retrieves the status of a specific VPN", 
               description = "Retrieves the status of a specific VPN by its ID",
               tags = {"01 - vpns"})
    public DetailedStatus getStatus(@PathVariable String id) throws IOException {
        return service.getStatus(id);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Starts a specific VPN", 
               description = "Starts a specific VPN by its ID",
               tags = {"02 - start/stop"})
    public ResponseEntity<Void> startVpn(@PathVariable String id) throws IOException {
        service.start(id);
        return ResponseEntity.accepted().header(HttpHeaders.LOCATION, "/vpns/" + id + "/status").build();
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stops a specific VPN", 
               description = "Stops a specific VPN by its ID",
               tags = {"02 - start/stop"})
    public Void stopVpn(@PathVariable String id) throws IOException {
        service.stop(id);
        return null;
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "List all VPN users", 
               description = "List all VPN users",
               tags = {"03 - users"})
    public List<String> listVpnUsers(@PathVariable String id) throws IOException {
        return service.listUsers(id);
    }

    @PostMapping("/{id}/users/{user}")
    @Operation(summary = "Create a specific VPN user", 
               description = "Create a specific VPN user by its ID and name",
               tags = {"03 - users"})
    public ResponseEntity<?> createVpnUser(@PathVariable String id, @PathVariable String user) throws IOException {
        service.createUser(id, user);

        return ResponseEntity.created(linkTo(methodOn(VpnController.class).getVpn(id)).toUri()).body(Map.of("user", user));
    }

    @GetMapping("/{id}/users/{user}")
    @Operation(summary = "Retrieves a specific VPN user", 
               description = "Retrieves a specific VPN user by its ID and name",
               tags = {"03 - users"})
    public String getVpnUser(@PathVariable String id, @PathVariable String user) throws IOException {
        throw new UnsupportedOperationException("Unimplemented method 'getVpnUser'");
    }

    @DeleteMapping("/{id}/users/{user}")
    @Operation(summary = "Deletes a specific VPN user", 
               description = "Deletes a specific VPN user by its ID and name",
               tags = {"03 - users"})
    public Void deleteVpnUser(@PathVariable String id, @PathVariable String user) throws IOException {
        service.deleteUser(id, user);
        return null;
    }
}
