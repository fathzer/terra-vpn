package com.fathzer.odvpn.providers.utils;

public class BasicVPSSettings {
    private String region;
    private String instanceType;
    private String sshKeyName;
    private String sshUser;
    
    public String getInstanceType() {
        return instanceType;
    }

    public String getRegion() {
        return region;
    }

    public String getSshKeyName() {
        return sshKeyName==null ? "odvpn" : sshKeyName;
    }

    public String getSshUser() {
        return sshUser==null ? "root" : sshUser;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public void setSshKeyName(String sshKey) {
        this.sshKeyName = sshKey;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    public String getRegion(String defaultRegion) {
        return region == null ? defaultRegion : region;
    }

    public String getInstanceType(String defaultInstanceType) {
        return instanceType == null ? defaultInstanceType : instanceType;
    }

}