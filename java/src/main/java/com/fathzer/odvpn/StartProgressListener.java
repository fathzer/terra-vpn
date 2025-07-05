package com.fathzer.odvpn;

public interface StartProgressListener {
    default void creatingVPS(VPSProvider.VPSState state) {}

    default void updatingDDNS(String hostName, String ip) {}

    default void waitingSSHConnection(String ip) {}

    default void restoringOpenVPNConfiguration() {}

    default void creatingOpenVPNConfiguration() {}

    default void startingOpenVPNServer() {}

    default void waitingDNSPropagation() {}

    default void ready() {}
}
