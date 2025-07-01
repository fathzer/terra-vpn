package com.fathzer.odvpn.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fathzer.odvpn.repository.InstanceParameters;

/** A VPN */
public class Vpn {
    private final String id;
    private VPNStatus status;
    private InstanceParameters instanceParameters;
    
    /**
     * Creates a new VPN with the given ID
     * <br>The status is set to {@link VPNStatus#STOPPED}
     * @param id the VPN ID
     */
    public Vpn(String id, InstanceParameters instanceParameters) {
        this.id = id;
        this.instanceParameters = instanceParameters;
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

    /**
     * Returns the VPN status
     * @return the VPN status
     */
    @JsonProperty("status")
    public VPNStatus status() {
        return status;
    }

    @JsonProperty("settings")
    public InstanceParameters instanceParameters() {
        return instanceParameters;
    }

    /**
     * Sets the VPN status
     * @param status the VPN status
     */
    public void setStatus(VPNStatus status) {
        this.status = status;
    }
}
