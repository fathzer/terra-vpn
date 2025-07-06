package com.fathzer.odvpn.utils;

/**
 * Utility class for validating DNS names.
 */
public final class DnsNameValidator {
    private DnsNameValidator() {
        // Prevent instantiation
    }
    
    /**
     * Validates if the given string is a valid DNS name.
     * A valid DNS name must:
     * - Be 1-253 characters long
     * - Consist of labels separated by dots
     * - Each label must be 1-63 characters long
     * - Each label must start and end with an alphanumeric character
     * - Each label can contain alphanumeric characters and hyphens (but not at start/end)
     * - Must have at least one dot (no TLD-only names like 'com')
     * 
     * @param name the DNS name to validate
     * @return true if the name is a valid DNS name, false otherwise
     */
    public static boolean isValid(String name) {
        return hasValidStructure(name) && hasValidLabels(name);
    }
    
    /**
     * Checks if the DNS name has a valid overall structure.
     * 
     * @param name the DNS name to check
     * @return true if the structure is valid, false otherwise
     */
    private static boolean hasValidStructure(String name) {
        // Check overall length (1-253 chars)
        if (name == null || name.isEmpty() || name.length() > 253) {
            return false;
        }
        
        // Check for at least one dot and not starting/ending with dot
        return !(name.startsWith(".") || name.endsWith(".") || name.indexOf('.') == -1);
    }
    
    /**
     * Validates all labels in the DNS name.
     * 
     * @param name the DNS name with labels to validate
     * @return true if all labels are valid, false otherwise
     */
    private static boolean hasValidLabels(String name) {
        String[] labels = name.split("\\.");
        if (labels.length < 2) {
            return false; // At least one dot means at least 2 labels
        }
        
        // Validate all labels except the last one (TLD)
        for (int i = 0; i < labels.length - 1; i++) {
            if (!isValidLabel(labels[i])) {
                return false;
            }
        }
        
        // Validate TLD (last label)
        return isValidTld(labels[labels.length - 1]);
    }
    
    /**
     * Validates a single DNS label (non-TLD).
     * 
     * @param label the label to validate
     * @return true if the label is valid, false otherwise
     */
    private static boolean isValidLabel(String label) {
        // Check label length (1-63 chars)
        if (label.isEmpty() || label.length() > 63) {
            return false;
        }
        
        // Check first and last character are alphanumeric
        if (!Character.isLetterOrDigit(label.charAt(0)) || 
            !Character.isLetterOrDigit(label.charAt(label.length() - 1))) {
            return false;
        }
        
        // Check all characters are alphanumeric or hyphen
        for (int i = 1; i < label.length() - 1; i++) {
            char c = label.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-')) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validates the TLD (Top-Level Domain) label.
     * 
     * @param tld the TLD to validate
     * @return true if the TLD is valid, false otherwise
     */
    private static boolean isValidTld(String tld) {
        // TLD must be at least 2 characters
        if (tld.length() < 2) {
            return false;
        }
        
        // TLD must contain only letters
        for (int i = 0; i < tld.length(); i++) {
            if (!Character.isLetter(tld.charAt(i))) {
                return false;
            }
        }
        
        return true;
    }
}
