package com.fathzer.odvpn;

import static com.fathzer.odvpn.utils.TarGzUtils.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.fathzer.odvpn.repository.VPNConfig;
import com.fathzer.odvpn.repository.VPNConfig.Protocol;

final class VPNConfigValidator {
    private VPNConfigValidator() {}

    static List<String> check(VPNConfig config, Path openvpnConfigPath) throws IOException {
        final List<String> errors = new LinkedList<>();
        try (InputStream is = Files.newInputStream(openvpnConfigPath)) {
            if (!contains(is, "pki/issued/"+config.hostname()+".crt", false)) {
                errors.add("OpenVPN configuration file does not contain the certificate for " + config.hostname());
            }
        }
        try (InputStream is = Files.newInputStream(openvpnConfigPath)) {
            if (!contains(is, "pki/private/"+config.hostname()+".key", false)) {
                errors.add("OpenVPN configuration file does not contain the private key for " + config.hostname());
            }
        }
        try (InputStream is = Files.newInputStream(openvpnConfigPath)) {
            try (InputStream ovpnConfIs = getEntryStream(is, "openvpn.conf", false)) {
                if (ovpnConfIs == null) {
                    errors.add("OpenVPN configuration file does not contain openvpn.conf file");
                } else {
                    Protocol protocol = getProtocol(ovpnConfIs, errors);
                    if (protocol != config.protocol()) {
                        errors.add("OpenVPN configuration file and vpn configuration do not contain the same protocol");
                    }
                }
            }
        }
        return errors;
    }

    private static Protocol getProtocol(InputStream is, List<String> errors) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            Optional<String> protocolLine = reader.lines().filter(line -> line.startsWith("proto ")).findFirst();
            if (protocolLine.isEmpty()) {
                return Protocol.UDP;
            }
            String[] elements = protocolLine.get().split(" ");
            if (elements.length != 2) {
                errors.add("OpenVPN configuration file contains a malformed protocol line: " + protocolLine.get());
                return null;
            }
            String protocol = elements[1];
            try {
                return Protocol.valueOf(protocol.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("OpenVPN configuration file contains an unknown protocol " + protocol);
                return null;
            }
        }
    }
}
