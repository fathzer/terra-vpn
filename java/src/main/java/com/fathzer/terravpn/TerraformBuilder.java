package com.fathzer.terravpn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fathzer.terravpn.Configuration.SSHKeys;
import com.fathzer.terravpn.ssh.SSHUtils;

import java.io.UncheckedIOException;

public record TerraformBuilder(Configuration config, Path outputDir) {
    public TerraformBuilder {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("Output directory cannot be null");
        }
    }

    public void build() throws IOException {
        Files.createDirectories(outputDir.resolve("scripts"));
        Files.createDirectories(outputDir.resolve(".ssh"));
        write(outputDir.resolve("variables.tf"), this::buildVariables);
        write(outputDir.resolve("terraform.tfvars"), this::buildVariablesValues);
        write(outputDir.resolve("main.tf"), this::buildMainScript);
        write(outputDir.resolve(".ssh/id_rsa"), this::copySshPrivateKey);
        write(outputDir.resolve(".ssh/id_rsa.pub"), this::copySshPublicKey);
    }

    private void write(Path path, Consumer<Consumer<String>> lineGenerator) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            lineGenerator.accept(l -> {
                try {
                    writer.write(l + "\n");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    /**
     * Build the Terraform variables file (variables.tf)
     * @param output the consumer to write the variables to
     */
    public void buildVariables(Consumer<String> output){
        final InputStream is = getClass().getResourceAsStream("variables.tf");
        if (is == null) {
            throw new IllegalStateException("Missing Terraform variables file");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.lines().forEach(output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        output.accept("");
        config.vpsProvider().getVariablesDefinition().forEach(output);
    }

    /**
     * Build the Terraform variables values file (terraform.tfvars)
     * @param output the consumer to write the variables values to
     */
    public void buildVariablesValues(Consumer<String> output) {
        Map<String, Object> customConfig = config.config();
        toTerraformValues(customConfig, output);
        final Set<String> shouldBeIgnored = customConfig.keySet();
        config.vpsProvider().getDefaultConfig().entrySet().stream().filter(e -> !shouldBeIgnored.contains(e.getKey())).forEach(e -> toTerraformValue(e.getKey(), e.getValue(), output));
    }

    private void toTerraformValues(Map<String, Object> variables, Consumer<String> output) {
        variables.forEach((key, value) -> toTerraformValue(key, value, output));
    }

    private void toTerraformValue(String varName, Object obj, Consumer<String> output) {
        output.accept(varName + " = " + toValue(obj));
    }

    private String toValue(Object obj) {
        Class<? extends Object> objectClass = obj.getClass();
        if (objectClass == Integer.class) {
            return obj.toString();
        }
        if (objectClass == String.class) {
            return "\"" + obj.toString() + "\"";
        }
        if (List.class.isAssignableFrom(objectClass)) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            ((List<?>) obj).forEach(e -> {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(toValue(e));
            });
            sb.append("]");
            return sb.toString();
        }
        throw new IllegalArgumentException("Unsupported type: " + obj.getClass());
    }

    public void buildMainScript(Consumer<String> output) {
        final InputStream is = getClass().getResourceAsStream("main.tf");
        if (is == null) {
            throw new IllegalStateException("Missing Terraform main script file");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        	final Stream<String> lines = reader.lines();
            lines.flatMap(this::getScriptPart).forEach(output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Stream<String> getScriptPart(String line) {
        final String completed = "%vps_completed%";
        if (line.contains(completed)) {
            return Stream.of(line.replace(completed, config.vpsProvider().getCompletedResource()));
        }
        final String requiredProviders = "%required_providers%";
        if (line.trim().equals(requiredProviders)) {
            final int indent = line.indexOf("%");
            return config.vpsProvider().getTerraformProvider().stream().map(s -> " ".repeat(indent)+s);
        }
        final String script = "%vps_script%";
        if (line.contains(script)) {
            return config.vpsProvider().getTerraformScript().stream();
        }
        return Stream.of(line);
    }

    public void copySshPrivateKey(Consumer<String> output) {
        final SSHKeys sshKeys = config.sshKeys();
        if (sshKeys != null) {
            SSHUtils.formatKey(sshKeys.privateKey()).forEach(output);
        }
    }

    public void copySshPublicKey(Consumer<String> output) {
        final SSHKeys sshKeys = config.sshKeys();
        if (sshKeys != null) {
            output.accept(sshKeys.publicKey());
        }
    }
}
