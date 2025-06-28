package com.fathzer.odvpn;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VPSStateTest {
    @Test
    void testValidState() {
        // Test avec un état qui n'a pas besoin d'IP
        VPSProvider.VPSState state = new VPSProvider.VPSState("123", null, VPSProvider.Status.STARTING);
        assertNotNull(state);
        assertEquals("123", state.id());
        assertNull(state.ip());
        assertEquals(VPSProvider.Status.STARTING, state.status());

        // Test avec un état qui a besoin d'IP (IP_READY)
        state = new VPSProvider.VPSState("123", "192.168.1.1", VPSProvider.Status.IP_READY);
        assertEquals("123", state.id());
        assertEquals("192.168.1.1", state.ip());
        assertEquals(VPSProvider.Status.IP_READY, state.status());

        // Test avec un état qui a besoin d'IP (READY)
        state = new VPSProvider.VPSState("123", "10.0.0.1", VPSProvider.Status.READY);
        assertEquals("123", state.id());
        assertEquals("10.0.0.1", state.ip());
        assertEquals(VPSProvider.Status.READY, state.status());
    }

    @Test
    void testNullStatus() {
        // Test avec status null
        assertThrows(IllegalArgumentException.class,
            () -> new VPSProvider.VPSState("123", null, null),
            "status cannot be null");
    }

    @Test
    void testInvalidId() {
        // Test avec id null
        assertThrows(IllegalArgumentException.class, 
            () -> new VPSProvider.VPSState(null, null, VPSProvider.Status.STARTING),
            "id cannot be null or empty");

        // Test avec id vide
        assertThrows(IllegalArgumentException.class, 
            () -> new VPSProvider.VPSState("", null, VPSProvider.Status.STARTING),
            "id cannot be null or empty");

        // Test avec id ne contenant que des espaces
        assertThrows(IllegalArgumentException.class, 
            () -> new VPSProvider.VPSState("   ", null, VPSProvider.Status.STARTING),
            "id cannot be null or empty");
    }

    @Test
    void testInvalidIpForState() {
        // Test avec IP null pour un état qui en a besoin (IP_READY)
        assertThrows(IllegalArgumentException.class, 
            () -> new VPSProvider.VPSState("123", null, VPSProvider.Status.IP_READY),
            "ip cannot be null when state is IP_READY");

        // Test avec IP null pour un état qui en a besoin (READY)
        assertThrows(IllegalArgumentException.class, 
            () -> new VPSProvider.VPSState("123", null, VPSProvider.Status.READY),
            "ip cannot be null when state is READY");
    }

    @Test
    void testInvalidIpFormat() {
        // Test avec IP invalide
        String[] invalidIps = {
            "256.1.1.1",        // nombre > 255
            "1.2.3.4.5",        // trop de parties
            "1.2.3",            // pas assez de parties
            "1.2.3.",           // partie vide à la fin
            ".1.2.3",           // partie vide au début
            "1.2.3.4.5",        // trop de parties
            "1.2.3.256",        // nombre > 255
            "a.b.c.d",          // caractères non numériques
            " 1.2.3.4 ",        // espaces
            "1.2.3.4 ",         // espace à la fin
            " 1.2.3.4"          // espace au début
        };

        for (String ip : invalidIps) {
            assertThrows(IllegalArgumentException.class, 
                () -> new VPSProvider.VPSState("123", ip, VPSProvider.Status.IP_READY),
                "ip must be a valid IPv4 address");
        }
    }

    @Test
    void testValidIpFormats() {
        // Test avec différents formats d'IP valides
        String[] validIps = {
            "0.0.0.0",
            "255.255.255.255",
            "192.168.1.1",
            "10.0.0.1",
            "172.16.0.1"
        };

        for (String ip : validIps) {
            VPSProvider.VPSState state = new VPSProvider.VPSState("123", ip, VPSProvider.Status.IP_READY);
            assertEquals(ip, state.ip());
        }
    }
}
