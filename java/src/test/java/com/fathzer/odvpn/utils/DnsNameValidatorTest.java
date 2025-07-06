package com.fathzer.odvpn.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DnsNameValidatorTest {

    @Test
    void testNullInput() {
        assertFalse(DnsNameValidator.isValid(null));
    }

    @Test
    void testEmptyString() {
        assertFalse(DnsNameValidator.isValid(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "example.com",
        "sub.domain.co.uk",
        "a.bc",
        "valid-with-hyphen.example.com",
        "xn--mnchen-3ya.example.com" // IDN example
    })
    void testValidDnsNames(String dnsName) {
        assertTrue(DnsNameValidator.isValid(dnsName), "Should be valid: " + dnsName);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        // Invalid formats
        ".starts-with-dot.com",
        "ends-with-dot.com.",
        "no-dot",
        "-start-with-hyphen.com",
        "end-with-hyphen-.com",
        "double..dot.com",
        "invalid@char.com",
        "space in name.com",
        // Invalid TLD
        "example.c",
        "example.1com",
        "example.c0m",
        // Label too long
        ("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.com"),
        // Total length > 253
        ("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc.dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd")
    })
    void testInvalidDnsNames(String dnsName) {
        assertFalse(DnsNameValidator.isValid(dnsName), "Should be invalid: " + dnsName);
    }

    @Test
    void testIpAddresses() {
        // IP addresses should not be considered valid DNS names
        assertFalse(DnsNameValidator.isValid("192.168.1.1"));
        assertFalse(DnsNameValidator.isValid("2001:db8::1"));
    }

    @Test
    void testEdgeCases() {
        // Test with maximum length label (63 chars) and maximum total length (253 chars)
        String maxLabel = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 63 'a's
        StringBuilder b = new StringBuilder();
        String domain = "example.com";
        while (b.length() < 253) {
            b.append(maxLabel);
            b.append(".");
        }
        b.append(domain);
        String maxLengthDns = b.toString().substring(b.length()-253);
        assertEquals(253, maxLengthDns.length());
        assertTrue(DnsNameValidator.isValid(maxLengthDns), "Should accept maximum length DNS name");
        
        // Test with just over maximum length
        String tooLongDns = "a" + maxLengthDns;
        assertFalse(DnsNameValidator.isValid(tooLongDns), "Should reject DNS name over 253 chars");
    }
}
