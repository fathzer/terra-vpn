package com.fathzer.terravpn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fathzer.terravpn.repository.ObjectConfig;

public abstract class VPSProvider implements Provider {

    /**
     * Gets the definition of variables specifically required to configure the provider.
     * @return the variables definition required to configure the provider in .tf files Terraform format
     */
    public List<String> getVariablesDefinition() {
        List<String> variables = readResource(this, "-variables.tf", "Terraform variables");
        if (isProtocolVariablesRequired()) {
            // Warning: we need to create a new list because the original list is immutable
            variables = new ArrayList<>(variables);
            variables.addAll(readResourceByPath(this, "Terraform protocol variables", "protocol-variables.tf"));
        }
        return variables;
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
        final String path = provider.getClass().getSimpleName() + suffix;
        return readResourceByPath(provider, type, path);
    }

    private static <T extends Provider> List<String> readResourceByPath(T provider, String type, final String path) {
        final InputStream is = provider.getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Missing "+type+" file for provider " + provider.getClass().getSimpleName()+" ("+path+")");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Checks if the protocol and port terraform variables are required.
     * <br>Typically, it could be required to open ports in the firewall.
     * <br>When these variables are required, <i>protocol</i> and <i>port</i> variables are added to the Terraform variables files.
     * @return true if the protocol and port variables are required. Default is false.
     */
    protected boolean isProtocolVariablesRequired() {
        return false;
    }

    /**
     * Gets the extra initialization commands that are executed after the VPS is created.
     * @return the extra initialization commands
     */
    protected List<String> getExtraInitalizationCommands() {
        return List.of();
    }

    public static String getSSHUser(ObjectConfig<VPSProvider> config) {
        return config.config().getOrDefault(Constants.SSH_USER_VAR, "root");
    }
}
