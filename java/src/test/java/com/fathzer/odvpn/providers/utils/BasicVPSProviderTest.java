package com.fathzer.odvpn.providers.utils;

import com.fathzer.odvpn.VPSProvider.VPSState;
import com.fathzer.odvpn.providers.utils.AbstractVPSProviderClient.AuthenticationException;
import com.fathzer.odvpn.providers.utils.AbstractVPSProviderClient.ErrorResponseException;
import com.fathzer.odvpn.repository.VPNConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BasicVPSProviderTest {
    private BasicVPSProvider<BasicTokenAuthVPSSettings> provider;
    private BasicVPSProviderClient client;

    @BeforeEach
    void setUp() {
        client = mock(BasicVPSProviderClient.class);
        provider = new BasicVPSProvider<>() {
            @Override
            protected BasicVPSProviderClient getClient() {
                return client;
            }
            @Override
            protected String getDefaultRegion() {
                return "us-east-1";
            }
            @Override
            protected String getDefaultInstanceType() {
                return "t2.micro";
            }
            @Override
            public String name() {
                return "MockProvider";
            }
            @Override
            long getReadyWaitFrequencyMs() {
            	return 100L;
            }
        };
        BasicTokenAuthVPSSettings defaultSettings = mock(BasicTokenAuthVPSSettings.class);
        when(defaultSettings.getToken()).thenReturn("token");
        when(defaultSettings.getSshKeyName()).thenReturn("sshKey");
        provider.setSettings(defaultSettings);
    }

    @Test
    void testCheckConfiguration_missingToken() throws IOException {
        BasicTokenAuthVPSSettings settingsMock = mock(BasicTokenAuthVPSSettings.class);
        when(settingsMock.getToken()).thenReturn(null);
        when(settingsMock.getSshKeyName()).thenReturn("sshKey");
        provider.setSettings(settingsMock);
        List<String> errors = provider.checkConfiguration();
        assertTrue(errors.contains("Missing token"));
    }

    @Test
    void testCheckConfiguration_missingSshKeyName() throws IOException {
        BasicTokenAuthVPSSettings settingsMock = mock(BasicTokenAuthVPSSettings.class);
        when(settingsMock.getToken()).thenReturn("token");
        when(settingsMock.getSshKeyName()).thenReturn(null);
        provider.setSettings(settingsMock);
        List<String> errors = provider.checkConfiguration();
        assertTrue(errors.stream().anyMatch(e -> e.contains("SSH key name")));
    }

    @Test
    void testCheckConfiguration_valid() throws Exception {
        when(client.getSSHKeyId("sshKey")).thenReturn("keyId");
        doNothing().when(client).checkRegion("us-east-1");
        doNothing().when(client).checkInstanceType("us-east-1", "t2.micro");
        List<String> errors = provider.checkConfiguration();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testCheckConfiguration_authenticationException() throws Exception {
        when(client.getSSHKeyId("sshKey")).thenThrow(new AuthenticationException(401, "Auth failed"));
        List<String> errors = provider.checkConfiguration();
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Auth failed")));
    }

    @Test
    void testCreateVPS_success() throws Exception {
        when(client.getSSHKeyId(anyString())).thenReturn("keyId");
        when(client.create(any(), any())).thenReturn("vpsId");
        BasicVPSProvider.Status readyStatus = BasicVPSProvider.Status.READY;
        BasicVPSProvider.Status creatingStatus = BasicVPSProvider.Status.STARTING;
        BasicVPSProvider.VPSState readyState = mock(BasicVPSProvider.VPSState.class);
        when(readyState.status()).thenReturn(readyStatus);
        BasicVPSProvider.VPSState creatingState = mock(BasicVPSProvider.VPSState.class);
        when(creatingState.status()).thenReturn(creatingStatus);
        when(client.getState("vpsId")).thenReturn(creatingState, readyState);
        AtomicBoolean progressCalled = new AtomicBoolean(false);
        Consumer<BasicVPSProvider.VPSState> progress = state -> progressCalled.set(true);
        VPNConfig vpnConfig = mock(VPNConfig.class);
        BasicVPSProvider.VPSState result = provider.createVPS(vpnConfig, progress);
        assertEquals(readyState, result);
        assertTrue(progressCalled.get());
    }

    @Test
    void testExists_true() throws Exception {
        BasicVPSProvider.VPSState state = mock(BasicVPSProvider.VPSState.class);
        when(state.status()).thenReturn(BasicVPSProvider.Status.READY);
        when(client.getState("id")).thenReturn(state);
        assertTrue(provider.exists("id"));
    }

    @Test
    void testExists_false() throws Exception {
        BasicVPSProvider.VPSState state = mock(BasicVPSProvider.VPSState.class);
        when(state.status()).thenReturn(BasicVPSProvider.Status.STOPPED);
        when(client.getState("id")).thenReturn(state);
        assertFalse(provider.exists("id"));
    }

    @Test
    void testExists_notFound() throws Exception {
        when(client.getState("id")).thenThrow(new ErrorResponseException(404, "Not found"));
        assertFalse(provider.exists("id"));
    }

    @Test
    void testDeleteVPS() throws Exception {
        doNothing().when(client).delete("id");
        provider.deleteVPS("id");
        verify(client).delete("id");
    }

    @Test
    void testUsesSettingsValuesAndNotDefault() throws Exception {
        // Arrange: set up settings with specific region/instanceType
        BasicTokenAuthVPSSettings settingsMock = new BasicTokenAuthVPSSettings();
        settingsMock.setToken("token");
        settingsMock.setSshKeyName("sshKey");
        settingsMock.setRegion("custom-region");
        settingsMock.setInstanceType("custom-type");

        provider.setSettings(settingsMock);
        when(client.getSSHKeyId("sshKey")).thenReturn("keyId");
        doNothing().when(client).checkRegion(any());
        doNothing().when(client).checkInstanceType(any(), any());
        provider.checkConfiguration();
        // Assert: verify correct values passed to checkInstanceType
        verify(client).checkInstanceType("custom-region", "custom-type");

        // Assert: verify correct values passed to create
        VPNConfig vpnConfig = mock(VPNConfig.class);
        when(client.create(any(), any())).thenReturn("instanceId");
        when(client.getState(anyString())).thenReturn(new VPSState("id", "1.1.1.1", BasicVPSProvider.Status.READY));
        
        provider.createVPS(vpnConfig, s->{});
        verify(client).create(argThat(settings ->
            settings instanceof VPSCreationSettings
            && "custom-region".equals(((VPSCreationSettings)settings).region())
            && "custom-type".equals(((VPSCreationSettings)settings).instanceType())
        ), eq(vpnConfig));
        verify(client, atLeastOnce()).getSSHKeyId("sshKey");
    }
}
