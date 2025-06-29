package com.fathzer.odvpn.ws.config;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fathzer.odvpn.ws.VpnService;

@RestControllerAdvice
public class VpnExceptionHandler {

    @ExceptionHandler(VpnService.VpnException.class)
    public ResponseEntity<Map<String, Object>> handleUnknownVpn(VpnService.VpnException ex) {
        Map<String, Object> body = Map.of(
            "error", ex.getMessage(),
            "timestamp", Instant.now().toString()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
