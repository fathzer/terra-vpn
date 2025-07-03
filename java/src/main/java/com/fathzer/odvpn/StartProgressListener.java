package com.fathzer.odvpn;

public interface StartProgressListener {
    void creatingVPS(VPSProvider.VPSState state);

    void updatingDDNS(String hostName, String ip);

    void waitingSSHConnection(String ip);

    void restoringOpenVPNConfiguration();

    void creatingOpenVPNConfiguration();

    void startingOpenVPNServer();

    void waitingDNSPropagation();

    void ready();
}
