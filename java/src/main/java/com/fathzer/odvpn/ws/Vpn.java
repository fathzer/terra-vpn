package com.fathzer.odvpn.ws;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A VPN */
public class Vpn {
    private final String id;
    private VPNStatus status;
    private String protocol;
    private String hostName;
    private int port;
    
    /**
     * Creates a new VPN with the given ID
     * <br>The status is set to {@link VPNStatus#STOPPED}
     * @param id the VPN ID
     */
    public Vpn(String id, String protocol, String hostName, int port) {
        this.id = id;
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
        return id;
    }

    @JsonProperty("protocol")
    public String protocol() {
        return protocol;
    }

    @JsonProperty("hostName")
    public String hostName() {
        return hostName;
    }

    @JsonProperty("port")
    public int port() {
        return port;
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
