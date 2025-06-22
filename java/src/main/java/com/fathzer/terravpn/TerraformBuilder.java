package com.fathzer.terravpn;

import static com.fathzer.terravpn.Constants.*;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fathzer.terravpn.json.InstanceParametersParser;
import com.fathzer.terravpn.repository.InstanceParameters;
import com.fathzer.terravpn.repository.ObjectConfig;

import java.io.UncheckedIOException;

public record TerraformBuilder(InstanceParameters config, Path outputDir, Path sshPrivateKey) {
    public TerraformBuilder {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("Output directory cannot be null");
        }
        if (sshPrivateKey == null || !Files.isRegularFile(sshPrivateKey)) {
            throw new IllegalArgumentException("SSH private key cannot be null or not a file");
        }
    }

    public void build() throws IOException {
        Files.createDirectories(outputDir.resolve("scripts"));
        Files.createDirectories(outputDir.resolve(".ssh"));
        write(outputDir.resolve("variables.tf"), this::buildVariables);
        write(outputDir.resolve("terraform.tfvars"), this::buildVariablesValues);
        write(outputDir.resolve("main.tf"), this::buildMainScript);
        InstanceParametersParser.write(outputDir.resolve("config.json"), config);
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
        config.vps().provider().getVariablesDefinition().forEach(output);
    }

    /**
     * Build the Terraform variables values file (terraform.tfvars)
     * @param output the consumer to write the variables values to
     */
    public void buildVariablesValues(Consumer<String> output) {
        final ObjectConfig<VPSProvider> vpsConfig = config.vps();
        final Map<String, String> vpsConfigMap = vpsConfig.config();
        toTerraformValues(vpsConfigMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), output);
        final Set<String> shouldBeIgnored = vpsConfigMap.keySet();
        vpsConfig.provider().getDefaultConfig().entrySet().stream().filter(e -> !shouldBeIgnored.contains(e.getKey())).forEach(e -> toTerraformValue(e.getKey(), e.getValue(), output));
        if (vpsConfig.provider().isProtocolVariablesRequired()) {
            toTerraformValue(PROTOCOL_VAR, config.protocol(), output);
            toTerraformValue(PORT_VAR, config.port(), output);
        }
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
            return Stream.of(line.replace(completed, config.vps().provider().getCompletedResource()));
        }
        final String requiredProviders = "%required_providers%";
        if (line.trim().equals(requiredProviders)) {
            final int indent = line.indexOf("%");
            return config.vps().provider().getTerraformProvider().stream().map(s -> " ".repeat(indent)+s);
        }
        final String privateSshKeyPath = "%private_sshkey_path%";
        if (line.contains(privateSshKeyPath)) {
            return Stream.of(line.replace(privateSshKeyPath, sshPrivateKey.toAbsolutePath().toString()));
        }
        final String script = "%vps_script%";
        if (line.contains(script)) {
            return config.vps().provider().getTerraformScript().stream();
        }
        return Stream.of(line);
    }
}
