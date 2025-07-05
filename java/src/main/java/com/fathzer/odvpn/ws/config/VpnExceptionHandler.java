package com.fathzer.odvpn.ws.config;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.fathzer.odvpn.ws.VpnService;

@RestControllerAdvice
public class VpnExceptionHandler {

    @ExceptionHandler(VpnService.VpnException.class)
    public ResponseEntity<Object> handleUnknownVpn(VpnService.VpnException ex, WebRequest request) {
        final String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        final ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatus());
        if (acceptHeader == null || acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE) || acceptHeader.contains(MediaType.ALL_VALUE)) {
            response.contentType(MediaType.APPLICATION_JSON);
            return response.body(Map.of(
                "error", ex.getMessage(),
                "timestamp", Instant.now().toString()
            ));
        }
        if (acceptHeader.contains(MediaType.TEXT_PLAIN_VALUE)) {
            return response.contentType(MediaType.TEXT_PLAIN).body("Error: " + ex.getMessage());
        }
        return response.build();
    }
}
