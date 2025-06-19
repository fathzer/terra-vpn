package com.fathzer.terravpn.ssh;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class KeyGenerator {
    private static final KeyPairGenerator GENERATOR;

    static {
        try {
            GENERATOR = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private KeyGenerator() {
    }

    /**
     * Creates a new RSA key pair
     * @param keySize the size of the key (Supported values: 1024, 2048, 4096)
     * @param privatePath the path to the private key file
     * @param publicPath the path to the public key file
     * @throws IOException if an I/O error occurs (for instance, if the folder does not exist)
     * @throws IllegalArgumentException if the key size is not supported
     */
    public static void createRSAKeyPair(int keySize, Path privatePath, Path publicPath) throws IOException {
        try {
            GENERATOR.initialize(keySize);
        } catch (InvalidParameterException e) {
            throw new IllegalArgumentException("Invalid key size: " + keySize, e);
        }
        final KeyPair kp = GENERATOR.generateKeyPair();

        try (BufferedWriter writer = Files.newBufferedWriter(privatePath)) {
            writer.write("-----BEGIN PRIVATE KEY-----\n");
            writer.write(Base64.getMimeEncoder().encodeToString( kp.getPrivate().getEncoded()));
            writer.write("\n-----END PRIVATE KEY-----\n");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, "ssh-rsa");
        writeBigInt(out, ((RSAPublicKey)kp.getPublic()).getPublicExponent());
        writeBigInt(out, ((RSAPublicKey)kp.getPublic()).getModulus());

        String encoded = Base64.getEncoder().encodeToString(out.toByteArray());

        try (BufferedWriter writer = Files.newBufferedWriter(publicPath)) {
            writer.write("ssh-rsa " + encoded + " user@host\n");
        }
    }

    /**
     * Creates a new RSA key pair with a 2048 bits key
     * @param folder the folder where the key pair will be created (id_rsa will contain private key, id_rsa.pub will contain public key)
     * @throws IOException if an I/O error occurs (for instance, if the folder does not exist)
     */
    public static void createRSAKeyPair(Path folder) throws IOException {
        createRSAKeyPair(2048, folder.resolve("id_rsa"), folder.resolve("id_rsa.pub"));
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java com.fathzer.terravpn.ssh.KeyGenerator <folder>");
            System.exit(1);
        }
        Path target = Path.of(args[0]);
        Files.createDirectories(target);
        createRSAKeyPair(target);
        System.out.println("Key pair created in " + target);
    }

    private static void writeString(OutputStream os, String str) throws IOException {
        byte[] data = str.getBytes(StandardCharsets.UTF_8);
        writeInt(os, data.length);
        os.write(data);
    }

    private static void writeBigInt(OutputStream os, BigInteger val) throws IOException {
        byte[] data = val.toByteArray();
        writeInt(os, data.length);
        os.write(data);
    }

    private static void writeInt(OutputStream os, int value) throws IOException {
        os.write((value >>> 24) & 0xFF);
        os.write((value >>> 16) & 0xFF);
        os.write((value >>> 8) & 0xFF);
        os.write(value & 0xFF);
    }
}
