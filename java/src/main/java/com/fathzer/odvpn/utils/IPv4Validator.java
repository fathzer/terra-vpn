package com.fathzer.odvpn.utils;

/**
 * Validates if the given string is a valid IP address (IPv4).
 */
public class IPv4Validator {
    private IPv4Validator() {
    }
    
    /**
     * Validates if the given string is a valid IP address (IPv4).
     * 
     * @param ip the IP address to validate
     * @return true if the string is a valid IP address, false otherwise
     */
    public static boolean isValid(String ip) {
        return (ip != null) && ip.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");
    }
}
