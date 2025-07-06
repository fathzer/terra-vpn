package com.fathzer.odvpn.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IPv4ValidatorTest {

    @Test
    void testNullInput() {
        assertFalse(IPv4Validator.isValid(null), "Null input should be invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        // Valid IPv4 addresses
        "192.168.1.1",
        "0.0.0.0",
        "255.255.255.255",
        "10.0.0.1",
        "172.16.0.1",
        "8.8.8.8",
        "1.1.1.1"
    })
    void testValidIPv4Addresses(String ip) {
        assertTrue(IPv4Validator.isValid(ip), "Should be valid: " + ip);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        // Invalid formats
        "",
        " ",
        "192.168.1",
        "192.168.1.1.1",
        "192.168.1.",
        ".192.168.1.1",
        "192.168.1.1 ",
        " 192.168.1.1",
        "192 .168.1.1",
        
        // Out of range
        "256.1.1.1",
        "1.256.1.1",
        "1.1.256.1",
        "1.1.1.256",
        "-1.1.1.1",
        "1.-1.1.1",
        "1.1.-1.1",
        "1.1.1.-1",
        
        // Invalid characters
        "a.b.c.d",
        "192.168.01.1",
        "192.168.1.01",
        "192.168.1.1a",
        "192.168.1.1a",
        "192,168,1,1",
        
        // IPv6 addresses (should be rejected)
        "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
        "2001:db8::1",
        "::1",
        "::",
        
        // Edge cases
        "0.0.0.0.0",
        "0.0.0",
        "0",
        "0.0",
        "0.0.0",
        "0.0.0.0.0",
        "0.0.0.0.",
        
        // Special addresses (should be invalid as per current implementation)
        "localhost",
        "example.com",
        "192.168.1.1/24"
    })
    void testInvalidIPv4Addresses(String ip) {
        assertFalse(IPv4Validator.isValid(ip), "Should be invalid: " + ip);
    }

    @Test
    void testEdgeCases() {
        // Test with maximum and minimum values
        assertTrue(IPv4Validator.isValid("0.0.0.0"));
        assertTrue(IPv4Validator.isValid("255.255.255.255"));
        
        // Test with leading/trailing spaces (should be invalid)
        assertFalse(IPv4Validator.isValid(" 192.168.1.1"));
        assertFalse(IPv4Validator.isValid("192.168.1.1 "));
        
        // Test with octal numbers (should be invalid as per current implementation)
        assertFalse(IPv4Validator.isValid("0177.0.0.1"));  // Octal 0177 = 127 in decimal
        
        // Test with hexadecimal (should be invalid)
        assertFalse(IPv4Validator.isValid("0x7f.0.0.1"));  // 0x7f = 127 in decimal
    }
}
