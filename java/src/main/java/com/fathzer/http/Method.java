package com.fathzer.http;

/**
 * Enum for supported HTTP methods
 */
public enum Method {
    GET(false), POST(true), PATCH(true), PUT(true), DELETE(false), HEAD(false), OPTIONS(false);

    private final boolean hasBody;
    
    private Method(boolean hasBody) {
        this.hasBody = hasBody;
    }
    
    /** Checks if the request sent with this method can have a body
     * @return true if the request sent with this method can have a body
     */
    public boolean hasBody() {
        return hasBody;
    }
}