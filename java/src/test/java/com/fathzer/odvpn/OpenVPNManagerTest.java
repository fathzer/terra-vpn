package com.fathzer.odvpn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.repository.InstanceParameters;

import java.util.List;

@SuppressWarnings("java:S6068")
class OpenVPNManagerTest {

    public static class DummyVPSProvider extends VPSProvider<Object> {
        @Override public String name() { return "dummy"; }
        @Override public Class<Object> getConfigClass() { return Object.class; }
        @Override public void deleteVPS(String id) throws IOException { /* no-op for test */ }
        @Override public boolean exists(String id) { return false; }
        @Override public VPSState createVPS(VPNConfig vpnConfig, java.util.function.Consumer<VPSState> progress) { return null; }
        @Override public java.util.List<String> checkConfiguration() { return java.util.Collections.emptyList(); }
    }

    public static class DummyDDNSProvider extends DynamicDNSProvider<Object> {
        @Override public String name() { return "dummy"; }
        @Override public void updateDns(String hostName, String ip) throws IOException {
            // Does nothing
        }
        @Override public Class<Object> getConfigClass() { return Object.class; }
    }

    private OpenVPNManager manager;
    private com.fathzer.odvpn.ssh.Ssh mockSsh;

    @BeforeEach
    void setUp() throws Exception {
        mockSsh = Mockito.mock(com.fathzer.odvpn.ssh.Ssh.class);
        manager = new OpenVPNManager(() -> mockSsh);
        // Inject the mock Ssh into the manager
        Field sshField = OpenVPNManager.class.getDeclaredField("ssh");
        sshField.setAccessible(true);
        sshField.set(manager, mockSsh);
    }

    @Test
    void testDoSSHCommandIsCalled() throws Exception {
        when(mockSsh.exec(eq("echo test"), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        manager.doSSHCommand(mockSsh, "echo test");
        verify(mockSsh).exec(eq("echo test"), any(OutputStream.class), any(OutputStream.class));
    }

    @Test
    void testSaveSendsCorrectCommand() throws Exception {
        Path dummyPath = Path.of("/tmp/testfile");
        when(mockSsh.exec(any(String.class), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        doNothing().when(mockSsh).download(any(), any());
        manager.save(dummyPath);
        verify(mockSsh).exec(eq("sudo tar -czf " + OpenVPNManager.OPENVPN_TAR_GZ + " -C " + OpenVPNManager.OPENVPN_VPS_FOLDER + " ."), any(OutputStream.class), any(OutputStream.class));
        verify(mockSsh).download(eq(OpenVPNManager.OPENVPN_TAR_GZ), eq(dummyPath.toString()));
    }

    @Test
    void testRestoreSendsCorrectCommands() throws Exception {
        Path localFile = Path.of("/tmp/restorefile");
        when(mockSsh.exec(any(String.class), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        doNothing().when(mockSsh).upload(any(), any());
        when(mockSsh.exec(anyList(), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        manager.restore(localFile);
        verify(mockSsh).exec(eq("sudo rm -f " + OpenVPNManager.OPENVPN_TAR_GZ), any(OutputStream.class), any(OutputStream.class));
        verify(mockSsh).upload(eq(localFile.toString()), eq(OpenVPNManager.OPENVPN_TAR_GZ));
        verify(mockSsh).exec(argThat((List<String> cmds) -> cmds.contains("sudo rm -rf " + OpenVPNManager.OPENVPN_VPS_FOLDER)), any(OutputStream.class), any(OutputStream.class));
    }

    @Test
    void testInitRemoteSendsCorrectCommands() throws Exception {
        VPNConfig config = new VPNConfig("vpn.mydomain.com", new String[]{"1.2.3.4", "8.8.8.8"}, VPNConfig.Protocol.UDP, 1194);
        InstanceParameters params = new InstanceParameters(new DummyVPSProvider(), new DummyDDNSProvider(), config);
        when(mockSsh.exec(any(String.class), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        when(mockSsh.exec(anyList(), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        manager.initRemote(params);
        verify(mockSsh).exec(eq("sudo rm -rf " + OpenVPNManager.OPENVPN_VPS_FOLDER), any(OutputStream.class), any(OutputStream.class));
        verify(mockSsh, atLeast(3)).exec(any(String.class), any(OutputStream.class), any(OutputStream.class)); // covers 3 doSSHCommand calls
    }

    @Test
    void testAddUserSendsCorrectCommand() throws Exception {
        var user = new OpenVPNManager.User("foo", false, java.time.Instant.now());
        when(mockSsh.exec(any(String.class), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        OpenVPNManager managerSpy = spy(manager);
        doReturn(List.of(user)).when(managerSpy).getUsers();
        managerSpy.addUser("foo");
        verify(mockSsh).exec(eq("docker run -v " + OpenVPNManager.OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OpenVPNManager.OPENVPN_IMAGE + " easyrsa build-client-full foo nopass"), any(OutputStream.class), any(OutputStream.class));
    }

    @Test
    void testDeleteUserSendsCorrectCommand() throws Exception {
        var user = new OpenVPNManager.User("bar", true, java.time.Instant.now());
        when(mockSsh.exec(any(String.class), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        OpenVPNManager managerSpy = spy(manager);
        doReturn(List.of(user)).when(managerSpy).getUsers();
        managerSpy.deleteUser("bar");
        verify(mockSsh).exec(eq("echo yes | docker run -v " + OpenVPNManager.OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OpenVPNManager.OPENVPN_IMAGE + " ovpn_revokeclient bar"), any(OutputStream.class), any(OutputStream.class));
    }
}
