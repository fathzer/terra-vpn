package com.fathzer.odvpn;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;

import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.repository.InstanceParameters;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("java:S6068")
class OpenVPNManagerTest {

    public static class DummyVPSProvider extends VPSProvider<Object> {
        @Override public String name() { return "dummy"; }
        @Override public Class<Object> getConfigClass() { return Object.class; }
        @Override public void deleteVPS(String id) throws IOException { /* no-op for test */ }
        @Override public boolean exists(String id) { return false; }
        @Override public VPSState createVPS(VPNConfig vpnConfig, Consumer<VPSState> progress) { return null; }
        @Override public List<String> checkConfiguration() { return List.of(); }
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
        verify(mockSsh).exec(eq(java.util.Arrays.asList(
            "#!/bin/bash",
            "set -e",
            "sudo rm -rf " + OpenVPNManager.OPENVPN_VPS_FOLDER,
            "sudo mkdir -p " + OpenVPNManager.OPENVPN_VPS_FOLDER,
            "sudo tar -xzf " + OpenVPNManager.OPENVPN_TAR_GZ + " -C " + OpenVPNManager.OPENVPN_VPS_FOLDER,
            "rm " + OpenVPNManager.OPENVPN_TAR_GZ
        )), any(OutputStream.class), any(OutputStream.class));
    }

    @Test
    void testInitRemoteSendsCorrectCommands() throws Exception {
        VPNConfig config = new VPNConfig("vpn.mydomain.com", List.of("1.2.3.4", "8.8.8.8"), VPNConfig.Protocol.UDP, 1194);
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

        // Should throw UserAlreadyExistsException if user is already valid
        var validUser = new OpenVPNManager.User("foo", true, java.time.Instant.now());
        doReturn(List.of(validUser)).when(managerSpy).getUsers();
        assertThrows(OpenVPNManager.UserAlreadyExistsException.class, () -> managerSpy.addUser("foo"));
    }

    @Test
    void testStartSendsCorrectCommand() throws Exception {
        VPNConfig config = new VPNConfig("vpn.mydomain.com", List.of("1.2.3.4", "8.8.8.8"), VPNConfig.Protocol.TCP, 443);
        InstanceParameters params = new InstanceParameters(new DummyVPSProvider(), new DummyDDNSProvider(), config);
        when(mockSsh.exec(any(String.class), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        manager.start(params);
        String expectedCommand = String.format(
            "docker run -v %s:/etc/openvpn -d --name openvpn --restart unless-stopped -p %s:%s --cap-add=NET_ADMIN %s",
            OpenVPNManager.OPENVPN_VPS_FOLDER,
            config.port(),
            "1194/" + config.protocol(),
            OpenVPNManager.OPENVPN_IMAGE
        );
        verify(mockSsh).exec(eq(expectedCommand), any(OutputStream.class), any(OutputStream.class));
    }


    @Test
    void testGetUsersParsesValidAndInvalidUsers() throws Exception {
        String header = "name,begin,end,status";
        String validUser = "alice,Mon Jan 1 00:00:00 2024 GMT,Jan  1 00:00:00 2025 GMT,VALID";
        String revokedUser = "bob,Mon Jan 1 00:00:00 2024 GMT,Jan  1 00:00:00 2025 GMT,REVOKED";
        String output = header + "\n" + validUser + "\n" + revokedUser;

        when(mockSsh.exec(anyString(), any(OutputStream.class), any(OutputStream.class)))
            .thenAnswer(invocation -> {
                OutputStream os = invocation.getArgument(1);
                os.write(output.getBytes());
                return 0;
            });

        List<OpenVPNManager.User> users = manager.getUsers();
        verify(mockSsh).exec(eq("docker run -v " + OpenVPNManager.OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm kylemanna/openvpn ovpn_listclients"), any(OutputStream.class), any(OutputStream.class));

        assertEquals(2, users.size());
        assertEquals("alice", users.get(0).name());
        assertTrue(users.get(0).valid());
        assertEquals("bob", users.get(1).name());
        assertFalse(users.get(1).valid());

        // Check problems in ssh command
        when(mockSsh.exec(anyString(), any(OutputStream.class), any(OutputStream.class))).thenReturn(1);
        assertThrows(IOException.class, () -> manager.getUsers());
    }

    @Test
    void testGetUserConfigurationFile() throws Exception {
        String username = "alice";
        var user = new OpenVPNManager.User(username, true, java.time.Instant.now());
        OpenVPNManager managerSpy = spy(manager);
        doReturn(List.of(user)).when(managerSpy).getUsers();
        var result = managerSpy.getUserConfigurationFile(username);
        assertNotNull(result);
        assertThrows(OpenVPNManager.UnknownUserException.class, () -> managerSpy.getUserConfigurationFile("bob"));
    }

    @Test
    void testDeleteUserSendsCorrectCommand() throws Exception {
        var user = new OpenVPNManager.User("bar", true, java.time.Instant.now());
        when(mockSsh.exec(any(String.class), any(OutputStream.class), any(OutputStream.class))).thenReturn(0);
        OpenVPNManager managerSpy = spy(manager);
        doReturn(List.of(user)).when(managerSpy).getUsers();
        managerSpy.deleteUser("bar");
        verify(mockSsh).exec(eq("echo 'yes' | docker run -v " + OpenVPNManager.OPENVPN_VPS_FOLDER + ":/etc/openvpn --rm -i " + OpenVPNManager.OPENVPN_IMAGE + " ovpn_revokeclient bar"), any(OutputStream.class), any(OutputStream.class));

        // Should throw UnknownUserException if user does not exist
        doReturn(List.of()).when(managerSpy).getUsers();
        assertThrows(OpenVPNManager.UnknownUserException.class, () -> managerSpy.deleteUser("baz"));
    }
}
