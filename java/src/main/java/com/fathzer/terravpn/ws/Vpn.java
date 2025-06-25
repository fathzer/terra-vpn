package com.fathzer.terravpn.ws;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A VPN */
public class Vpn {
    private final String hostName;
    private final String protocol;
    private final int port;
    private VPNStatus status;
    
    /**
     * Creates a new VPN with the given ID
     * <br>The status is set to {@link VPNStatus#STOPPED}
     * @param id the VPN ID
     */
    public Vpn(String protocol, String hostName, int port) {
        this.hostName = hostName;
        this.protocol = protocol;
        this.port = port;
        this.status = VPNStatus.STOPPED;
    }

    /**
     * Returns the VPN ID
     * @return the VPN ID
     */
    @JsonProperty("id")
    public String id() {
        return protocol+"://"+ hostName+":"+ port;
    }

    /**
     * Returns the VPN status
     * @return the VPN status
     */
    @JsonProperty("status")
    public VPNStatus status() {
        return status;
    }

    /**
     * Sets the VPN status
     * @param status the VPN status
     */
    public void setStatus(VPNStatus status) {
        this.status = status;
    }
}
