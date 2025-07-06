package com.fathzer.odvpn.repository;

import com.fathzer.odvpn.utils.DnsNameValidator;
import com.fathzer.odvpn.utils.IPv4Validator;

/**
 * Represents the configuration for a VPN connection.
 * 
 * @param hostName the hostname or IP address of the VPN server (cannot be null, empty, or an IP address)
 * @param dnsServers an array of DNS server IP addresses to be used with the VPN (can be null or empty, if notshould have between 2 and 4 elements)
 * @param protocol the transport protocol to use (UDP or TCP)
 * @param port the port number to connect to (0 means use default port 1194)
 * @throws IllegalArgumentException if any of the parameters are invalid
 */
public record VPNConfig(String hostname, String[] dnsServers, Protocol protocol, int port) {
    /**
     * Transport protocol for the VPN connection.
     */
    public enum Protocol {
        /** User Datagram Protocol */
        UDP, 
        /** Transmission Control Protocol */
        TCP;
    }

    /**
     * Validates and creates a new VPN configuration.
     * 
     * @param hostName the hostname of the VPN server (cannot be null, empty, or an IP address)
     * @param dnsServers an array of DNS server IP addresses (can be null, should have between 2 and 4 elements)
     * @param protocol the transport protocol to use (if null, defaults to UDP)
     * @param port the port number (if 0, defaults to 1194; must be between 0 and 65535)
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public VPNConfig {
        if (hostname == null) {
            throw new IllegalArgumentException("Host name cannot be null");
        }
        
        if (hostname.trim().isEmpty()) {
            throw new IllegalArgumentException("Host name cannot be empty");
        }
        
        // Validate hostname format (DNS name validation)
        if (!DnsNameValidator.isValid(hostname)) {
            throw new IllegalArgumentException("Invalid DNS name format: " + hostname);
        }

        // Validate port number
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
        
        // Validate DNS servers
        if (dnsServers != null) {
            if (dnsServers.length!=0 && (dnsServers.length < 2 || dnsServers.length > 4)) {
                throw new IllegalArgumentException("DNS servers array must be empty or have between 2 and 4 elements");
            }
            for (String dns : dnsServers) {
                if (dns == null) {
                    throw new IllegalArgumentException("DNS server address cannot be null");
                }
                if (!IPv4Validator.isValid(dns)) {
                    throw new IllegalArgumentException("Invalid IP address for DNS server: " + dns);
                }
            }
        }

    }
    
    /**
     * Gets the port number, returning the default port (1194) if port is 0.
     * 
     * @return the port number to use
     */
    public int port() {
        return port == 0 ? 1194 : port;
    }

    /**
     * Gets the protocol, returning UDP if protocol is null.
     * 
     * @return the protocol to use
     */
    public Protocol protocol() {
        return protocol == null ? Protocol.UDP : protocol;
    }
}
