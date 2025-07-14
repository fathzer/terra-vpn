package com.fathzer.odvpn.providers.utils;

public class BasicTokenAuthVPSConfiguration extends BasicVPSConfiguration {
    private String token;
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
}
