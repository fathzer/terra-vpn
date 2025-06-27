package com.fathzer.odvpn.providers;

import static com.fathzer.terravpn.Constants.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fathzer.odvpn.AbstractVPSProviderClient.AuthenticationException;
import com.fathzer.odvpn.VPSProvider;
import com.fathzer.odvpn.providers.VultrClient.InstanceCreationRequest;
import com.fathzer.terravpn.repository.InstanceParameters;
import com.fathzer.terravpn.repository.ObjectConfig;
import com.fathzer.terravpn.utils.Registerable;

/**
 * Vultr VPS provider implementation.
 * This provider allows deploying OpenVPN servers on Vultr's cloud infrastructure.
 */
@Registerable(
        value = "vultr",
        classes = {VPSProvider.class}
)
public class VultrVPS extends VPSProvider {
    private static final String DEFAULT_INSTANCE_TYPE = "vc2-1c-0.5gb";
    private static final String DEFAULT_ZONE = "ewr";

    private static final String TOKEN_VAR = "token";
    private static final String SSH_KEY_NAME_VAR = "ssh_key_name";
    
    @Override
    public String name() {
        return "Vultr VPS";
    }

    @Override
    public List<String> checkConfiguration(ObjectConfig<VPSProvider> config) throws IOException, InterruptedException {
        List<String> errors = new LinkedList<>();
        String token = config.config().get(TOKEN_VAR);
        if (token == null) {
            errors.add("Missing token");
        }
        String sshKeyName = config.config().get(SSH_KEY_NAME_VAR);
        if (sshKeyName == null || sshKeyName.trim().isEmpty()) {
            errors.add("Missing SSH key name or SSH key name is empty");
        }
        if (!errors.isEmpty()) {
            return errors;
        }

        try (VultrClient client = new VultrClient(token)) {
            // Check if the SSH key exists
            try {
                client.getSSHKeyId(sshKeyName);
            } catch (AuthenticationException e) {
                errors.add(e.getMessage());
                return errors;
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
            // Check if zone exists
            String zone = config.config().getOrDefault(ZONE_VAR, DEFAULT_ZONE);
            try {
                client.checkZone(zone);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
            // Check if instance type exists in the zone
            try {
                client.checkInstanceType(zone, config.config().getOrDefault(INSTANCE_TYPE_VAR, DEFAULT_INSTANCE_TYPE));
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    @Override
    public VPSState createVPS(InstanceParameters parameters, Consumer<String> progress) throws IOException {
        Map<String, String> config = parameters.vps().config();
        try (VultrClient client = new VultrClient(config.get(TOKEN_VAR))) {
            String sshKeyId = client.getSSHKeyId(config.get(SSH_KEY_NAME_VAR));
            InstanceCreationRequest request = new InstanceCreationRequest(
                config.getOrDefault(ZONE_VAR, DEFAULT_ZONE),
                config.getOrDefault(INSTANCE_TYPE_VAR, DEFAULT_INSTANCE_TYPE),
                "TODO-InstanceName",
                "docker", "disabled", List.of("On demand VPN"), List.of(sshKeyId));   
        
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createVPS'");
        }
    }

    @Override
    public void deleteVPS(InstanceParameters parameters, String id) throws IOException {
        try (VultrClient client = new VultrClient(parameters.vps().config().get(TOKEN_VAR))) {
            client.deleteVPS(id);
        }
    }
}
