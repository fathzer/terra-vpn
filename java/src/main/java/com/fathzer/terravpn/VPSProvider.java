package com.fathzer.terravpn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public abstract class VPSProvider implements Provider {

    /**
     * Gets the definition of variables specifically required to configure the provider.
     * <br>It is recommended that the variables have a name prefixed by the provider ID.
     * @return the variables definition required to configure the provider in .tf files Terraform format
     */
    public List<String> getVariablesDefinition() {
        return readResource(this, "-variables.tf", "Terraform variables");
    }

    /**
     * Gets the default configuration for the provider.
     * @return a map between a variable name and its default value
     */
    public Map<String, Object> getDefaultConfig() {
        return Map.of();
    }


    public List<String> getTerraformProvider() {
        return readResource(this, "-providers.tf", "Terraform provider");
    }
    public List<String> getTerraformScript() {
        return readResource(this, "-script.tf", "Terraform script");
    }

    public abstract String getCompletedResource();

    protected static <T extends Provider> List<String> readResource(T provider, String suffix, String type) {
        final String path = provider.id() + suffix;
        final InputStream is = provider.getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Missing "+type+" file for provider " + provider.id()+" ("+path+")");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
