package com.fathzer.odvpn.ssh;

import static org.junit.jupiter.api.Assertions.*;


import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.*;

class KeyGeneratorTest {
    private static Path tmpDir;

    @BeforeAll
    static void setUp() throws IOException {
        tmpDir = Files.createTempDirectory("keygenTest");
    }

    @AfterAll
    static void tearDown() throws IOException {
        for (Path dir: Files.list(tmpDir).toList()) {
            Files.delete(dir);
        }
        Files.delete(tmpDir);
    }

    @Test
    @SuppressWarnings("squid:S5778")
    void testCreateRSAKeyPair() throws IOException {
        assertThrows (IllegalArgumentException.class, () -> KeyGenerator.createRSAKeyPair(-4, tmpDir.resolve("wrong_rsa"), tmpDir.resolve("wrong_rsa.pub")));
        KeyGenerator.createRSAKeyPair(tmpDir);

        // Test private key
        Path privateKeyPath = tmpDir.resolve("id_rsa");
        assertTrue(Files.exists(privateKeyPath));
        List<String> privateKeyLines = Files.readAllLines(privateKeyPath);
        assertTrue (privateKeyLines.size() > 2);
        testHeaderFooter(privateKeyLines.get(0), true);
        testHeaderFooter(privateKeyLines.get(privateKeyLines.size() - 1), false);
        for (int i=1; i<privateKeyLines.size() - 1; i++) {
            String line = privateKeyLines.get(i);
            assertTrue (!line.isEmpty() && line.length() <= 76, "Wrong line size: " + line);
            assertTrue (line.matches("^[A-Za-z0-9+/=]+$"), "Illegal characters in line: " + line);
        }

        // Test public key
        testPublicKey(tmpDir.resolve("id_rsa.pub"));
    }

    void testHeaderFooter(String line, boolean header) {
        final String message = "Wrong " + (header ? "Header":"Footer") + " line: " + line;
        assertTrue(line.contains(header ? "BEGIN PRIVATE KEY" : "END PRIVATE KEY"), message);
        assertEquals('-', line.charAt(0), message);
        assertEquals('-', line.charAt(line.length() - 1), message);
    }

    void testPublicKey(Path pub) throws IOException {
        assertTrue(Files.exists(pub));
        String pubStr = Files.readString(pub).trim();
        assertTrue(pubStr.startsWith("ssh-rsa "), "Key must start with 'ssh-rsa'");

        String[] parts = pubStr.split(" ");
        assertEquals(3, parts.length, "Key line must contain three parts: type, base64, comment");

        String type = parts[0];
        String b64data = parts[1];

        assertEquals("ssh-rsa", type, "Expected key type: ssh-rsa");

        byte[] raw = Base64.getDecoder().decode(b64data);
        ByteBuffer buf = ByteBuffer.wrap(raw);

        String readType = readSSHString(buf);
        assertEquals("ssh-rsa", readType, "Key's encoded type must be 'ssh-rsa'");

        BigInteger e = readSSHBigInt(buf);
        BigInteger n = readSSHBigInt(buf);

        assertNotNull(e, "Exponent 'e' should not be null");
        assertNotNull(n, "Modulus 'n' should not be null");
        System.out.println(n.bitLength());
        assertTrue(n.bitLength() >= 2048, "Modulus length should be >= 2048 bits");
    }

    private String readSSHString(ByteBuffer buf) {
        int len = buf.getInt();
        byte[] strBytes = new byte[len];
        buf.get(strBytes);
        return new String(strBytes, StandardCharsets.UTF_8);
    }

    private BigInteger readSSHBigInt(ByteBuffer buf) {
        int len = buf.getInt();
        byte[] val = new byte[len];
        buf.get(val);
        return new BigInteger(val);
    }
}
