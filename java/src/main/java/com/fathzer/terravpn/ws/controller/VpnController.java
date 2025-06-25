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

import org.springframework.http.HttpStatus;
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

    @PostMapping
    @Operation(summary = "Creates a new VPN configuration", 
               description = "Creates a new VPN configuration without starting it")
    public ResponseEntity<Vpn> createVpn(@RequestBody InstanceParameters vpnDto) {
        Vpn saved = service.save(vpnDto);
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
    public ResponseEntity<Vpn> getVpn(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a specific VPN configuration", 
               description = "Updates a specific VPN configuration by its ID")
    public ResponseEntity<Vpn> updateVpn(@PathVariable Long id, @RequestBody InstanceParameters vpnDto) {
        return service.update(id, vpnDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes a specific VPN configuration", 
               description = "Deletes a specific VPN configuration by its ID")
    public ResponseEntity<Void> deleteVpn(@PathVariable Long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Retrieves the status of a specific VPN", 
               description = "Retrieves the status of a specific VPN by its ID")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable Long id) {
        return service.getStatus(id)
                .map(status -> Map.of("status", status.name()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Starts a specific VPN", 
               description = "Starts a specific VPN by its ID")
    public ResponseEntity<Map<String, String>> startVpn(@PathVariable Long id) {
        return service.startVpn(id)
                .map(status -> ResponseEntity.accepted().body(Map.of("status", status.name())))
                .orElse(ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "VPN already running or not found")));
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stops a specific VPN", 
               description = "Stops a specific VPN by its ID")
    public ResponseEntity<Map<String, String>> stopVpn(@PathVariable Long id) {
        return service.stopVpn(id)
                .map(status -> ResponseEntity.accepted().body(Map.of("status", status.name())))
                .orElse(ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "VPN already stopped or not found")));
    }
}
