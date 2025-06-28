package com.fathzer.terravpn.ws.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fathzer.terravpn.repository.InstanceParameters;
import com.fathzer.terravpn.ws.Vpn;
import com.fathzer.terravpn.ws.VpnService;

import io.swagger.v3.oas.annotations.Operation;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("api/vpns")
public class VpnController {
    private final VpnService service;

    public VpnController(VpnService service) {
        this.service = service;
    }

    @PostMapping("/{id}")
    @Operation(summary = "Creates a new VPN configuration", 
               description = "Creates a new VPN configuration without starting it")
    public ResponseEntity<?> createVpn(@PathVariable String id, @RequestBody InstanceParameters vpnDto) {
        Vpn saved = service.create(id, vpnDto);
        URI location = URI.create("/vpns/" + saved.id());
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping
    @Operation(summary = "Lists all VPN configurations", 
               description = "Lists all VPN configurations")
    public List<Vpn> getAllVpns() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieves a specific VPN configuration", 
               description = "Retrieves a specific VPN configuration by its ID")
    public Vpn getVpn(@PathVariable String id) {
        return service.findVpnById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a specific VPN configuration", 
               description = "Updates a specific VPN configuration by its ID")
    public ResponseEntity<Vpn> updateVpn(@PathVariable String id, @RequestBody InstanceParameters vpnDto) {
        return ResponseEntity.ok(service.update(id, vpnDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes a specific VPN configuration", 
               description = "Deletes a specific VPN configuration by its ID")
    public Void deleteVpn(@PathVariable String id) {
        service.delete(id);
        return null;
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Retrieves the status of a specific VPN", 
               description = "Retrieves the status of a specific VPN by its ID")
    public Map<String, String> getStatus(@PathVariable String id) {
        return Map.of("status", service.getStatus(id).name());
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Starts a specific VPN", 
               description = "Starts a specific VPN by its ID")
    public Map<String, String> startVpn(@PathVariable String id) {
        return Map.of("status", service.startVpn(id).name());
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stops a specific VPN", 
               description = "Stops a specific VPN by its ID")
    public Map<String, String> stopVpn(@PathVariable String id) {
        return Map.of("status", service.stopVpn(id).name());
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "List all VPN users", 
               description = "List all VPN users")
    public List<String> listVpnUsers(@PathVariable String id) {
        return service.listUsers(id);
    }

    @PostMapping("/{id}/users/{user}")
    @Operation(summary = "Create a specific VPN user", 
               description = "Create a specific VPN user by its ID and name")
    public ResponseEntity<?> createVpnUser(@PathVariable String id, @PathVariable String user) {
        service.createUser(id, user);

        URI location = URI.create("/vpns/" + id + "/users/" + user);
        return ResponseEntity.created(location).body(Map.of("user", user));
    }

    @DeleteMapping("/{id}/users/{user}")
    @Operation(summary = "Deletes a specific VPN user", 
               description = "Deletes a specific VPN user by its ID and name")
    public Void deleteVpnUser(@PathVariable String id, @PathVariable String user) {
        service.deleteUser(id, user);
        return null;
    }
}
