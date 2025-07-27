package com.fathzer.odvpn.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

class VPNConfigTest {
    private static final List<String> EMPTY_LIST = List.of();

    @Test
    void testValidConfig() {
        // Test with all parameters specified
        VPNConfig config = new VPNConfig("example.com", List.of("8.8.8.8", "9.9.9.9"), VPNConfig.Protocol.TCP, 443);
        assertEquals("example.com", config.hostname());
        assertEquals(List.of("8.8.8.8", "9.9.9.9"), config.dnsServers());
        assertEquals(VPNConfig.Protocol.TCP, config.protocol());
        assertEquals(443, config.port());
        
        // Test with default values
        config = new VPNConfig("vpn.example.org", EMPTY_LIST, null, 0);
        assertEquals("vpn.example.org", config.hostname());
        assertEquals(EMPTY_LIST, config.dnsServers());
        assertEquals(VPNConfig.Protocol.UDP, config.protocol());
        assertEquals(1194, config.port()); // Default port should be used
        
        // Test with a valid subdomain
        assertDoesNotThrow(() -> new VPNConfig("sub.domain.co.uk", EMPTY_LIST, null, 0));
    }

    @Test
    void testHostNameValidation() {
        // Null hostname
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig(null, EMPTY_LIST, null, 0));
            
        // Empty hostname
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("", EMPTY_LIST, null, 0));
            
        // Hostname with invalid characters
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("example$.com", EMPTY_LIST, null, 0));
            
        // Hostname starting/ending with dot
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig(".example.com", EMPTY_LIST, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("example.com.", EMPTY_LIST, null, 0));
            
        // IP addresses are not allowed as hostnames
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("192.168.1.1", EMPTY_LIST, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("2001:db8::1", EMPTY_LIST, null, 0));
            
        // Must have at least one dot
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("localhost", EMPTY_LIST, null, 0));
            
        // TLD must be at least 2 characters
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("example.c", EMPTY_LIST, null, 0));
            
        // Labels must be 63 chars or less
        String longLabel = "a".repeat(64);
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig(longLabel + ".com", EMPTY_LIST, null, 0));
    }

    @Test
    void testDnsServersValidation() {
        // Null DNS server in array
        final List<String> nullString = new LinkedList<String>();
        nullString.add(null);
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("example.com", nullString, null, 0));
            
        // Invalid IP address
        final List<String> wrongDnsServers = List.of("300.400.500.600");
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("example.com", wrongDnsServers, null, 0));
            
        // Valid IPv4 only (IPv6 should be rejected)
        assertDoesNotThrow(() -> new VPNConfig("example.com", List.of("8.8.8.8", "1.1.1.1"), null, 0));
            
        // Empty array is allowed
        assertDoesNotThrow(() -> new VPNConfig("example.com", EMPTY_LIST, null, 0));
            
        // IPv6 addresses should be rejected
        final List<String> ipv6Addresses = List.of("2001:4860:4860::8888", "::1", "2001:0db8:85a3:0000:0000:8a2e:0370:7334", "2001:db8::8a2e:370:7334");
        for (String ipv6 : ipv6Addresses) {
            final List<String> ipDnsServers = List.of(ipv6);
            assertThrows(IllegalArgumentException.class,
                () -> new VPNConfig("example.com", ipDnsServers, null, 0),
                "Should reject IPv6 address: " + ipv6);
        }
    }

    @Test
    void testPortValidation() {
        // Valid ports
        assertDoesNotThrow(() -> new VPNConfig("example.com", EMPTY_LIST, null, 0));
        assertDoesNotThrow(() -> new VPNConfig("example.com", EMPTY_LIST, null, 1));
        assertDoesNotThrow(() -> new VPNConfig("example.com", EMPTY_LIST, null, 65535));
        
        // Invalid ports
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("example.com", EMPTY_LIST, null, -1));
        assertThrows(IllegalArgumentException.class, () -> new VPNConfig("example.com", EMPTY_LIST, null, 65536));
    }

    @Test
    void testHostNameEdgeCases() {
        // Minimum valid hostname with TLD
        assertDoesNotThrow(() -> new VPNConfig("a.bc", EMPTY_LIST, null, 0));
        
        // Valid hostname with numbers, hyphens, and multiple subdomains
        assertDoesNotThrow(() -> new VPNConfig("vpn-123.sub-domain.example-456.com", EMPTY_LIST, null, 0));
        
        // Hostname with maximum length (253 characters total)
        String domain = "example.com";
        String label = "a".repeat(63); // Max label length is 63 chars
        String maxLengthHostname = String.format("%s.%s.%s", label, label, domain);
        assertTrue(maxLengthHostname.length() <= 253);
        assertDoesNotThrow(() -> new VPNConfig(maxLengthHostname, EMPTY_LIST, null, 0));
        
        // Test with a valid internationalized domain name (IDN)
        assertDoesNotThrow(() -> new VPNConfig("münchen.example.com", EMPTY_LIST, null, 0));
    }

    @Test
    void testDefaultValues() {
        VPNConfig config = new VPNConfig("example.com", EMPTY_LIST, null, 0);
        assertEquals(VPNConfig.Protocol.UDP, config.protocol());
        assertEquals(1194, config.port());
        
        // Explicit protocol should override default
        config = new VPNConfig("example.com", EMPTY_LIST, VPNConfig.Protocol.TCP, 0);
        assertEquals(VPNConfig.Protocol.TCP, config.protocol());
        
        // Explicit port should override default
        config = new VPNConfig("example.com", EMPTY_LIST, null, 443);
        assertEquals(443, config.port());
    }
}
