package com.fathzer.terravpn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

public record Configuration(String name, VPSProvider vpsProvider, DynamicDNSProvider ddnsProvider, Map<String, Object> config) {
    public Configuration {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (vpsProvider == null) {
            throw new IllegalArgumentException("VPS provider cannot be null");
        }
        if (ddnsProvider == null) {
            throw new IllegalArgumentException("DDNS provider cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
    }

    public static Configuration fromJson(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            final JSONObject json = new JSONObject(new JSONTokener(is));
            return new Configuration(json.getString("name"), (VPSProvider)ProviderRegistry.getProvider(json.getString("vpsProvider")), (DynamicDNSProvider)ProviderRegistry.getProvider(json.getString("ddnsProvider")), json.getJSONObject("config").toMap());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
