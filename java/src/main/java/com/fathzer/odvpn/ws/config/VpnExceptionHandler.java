package com.fathzer.odvpn.ws.config;

import java.time.Instant;
import java.util.function.Function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.fathzer.odvpn.OpenVPNManager;
import com.fathzer.odvpn.ws.VpnService;
import com.fathzer.odvpn.ws.controller.ErrorObject;

@RestControllerAdvice
public class VpnExceptionHandler {

    @ExceptionHandler(VpnService.VpnException.class)
    public ResponseEntity<Object> handleUnknownVpn(VpnService.VpnException ex, WebRequest request) {
        return handleException(ex, request, VpnService.VpnException::getStatus, VpnService.VpnException::getMessage);
    }

    @ExceptionHandler(OpenVPNManager.UnknownUserException.class)
    public ResponseEntity<Object> handleUnknownUser(OpenVPNManager.UnknownUserException ex, WebRequest request) {
        return handleException(ex, request, e -> HttpStatus.NOT_FOUND, e -> "User " + e.getMessage() + " not found");
    }

    @ExceptionHandler(OpenVPNManager.UserAlreadyExistsException.class)
    public ResponseEntity<Object> handleUserAlreadyExists(OpenVPNManager.UserAlreadyExistsException ex, WebRequest request) {
        return handleException(ex, request, e -> HttpStatus.CONFLICT, e -> "User " + e.getMessage() + " already exists");
    }

    private <T extends Throwable> ResponseEntity<Object> handleException(T ex, WebRequest request, Function<T, HttpStatus> statusFunction, Function<T, String> messageFunction) {
        final String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
        final ResponseEntity.BodyBuilder response = ResponseEntity.status(statusFunction.apply(ex));
        if (acceptHeader == null || acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE) || acceptHeader.contains(MediaType.ALL_VALUE)) {
            response.contentType(MediaType.APPLICATION_JSON);
            return response.body(new ErrorObject(messageFunction.apply(ex), Instant.now().toString()));
        }
        if (acceptHeader.contains(MediaType.TEXT_PLAIN_VALUE)) {
            return response.contentType(MediaType.TEXT_PLAIN).body("Error: " + messageFunction.apply(ex));
        }
        return response.build();
    }
}
