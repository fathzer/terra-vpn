package com.fathzer.odvpn;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import com.fathzer.terravpn.Constants;
import com.fathzer.terravpn.repository.InstanceParameters;
import com.fathzer.terravpn.repository.ObjectConfig;

public abstract class VPSProvider implements Provider {

    /**
     * Represents the state of a Virtual Private Server (VPS) instance.
     * This record holds essential identifying information about a VPS.
     *
     * @param id The unique identifier of the VPS instance in the provider's system
     * @param ip The public IP address assigned to the VPS instance
     */
    public record VPSState(String id, String ip) {}

    /**
     * Checks the configuration for the VPS provider.
     * This method is used to validate the configuration before creating a VPS instance.
     *
     * @param config The configuration object containing the provider-specific settings
     * @return A list of configuration errors, or an empty list if the configuration is valid
     */
    public abstract List<String> checkConfiguration(ObjectConfig<VPSProvider> config) throws IOException, InterruptedException;

    /**
     * Creates a new Virtual Private Server (VPS) instance.
     * This method is responsible for provisioning a new VPS instance based on the provided parameters.
     *
     * @param parameters The parameters defining the VPS configuration, such as instance type, storage, and network settings
     * @param progress A consumer that receives progress updates during the VPS creation process
     * @return A VPSState object representing the created VPS instance, containing its unique identifier and public IP address
     */
    public abstract VPSState createVPS(InstanceParameters parameters, Consumer<String> progress) throws IOException;

    /**
     * Deletes an existing Virtual Private Server (VPS) instance.
     * This method is responsible for removing a VPS instance from the provider's system.
     *
     * @param parameters The parameters defining the VPS configuration, such as instance type, storage, and network settings
     * @param id The unique identifier of the VPS instance to be deleted
     */
    public abstract void deleteVPS(InstanceParameters parameters, String id) throws IOException;

    /**
     * Retrieves the SSH username for the VPS provider.
     * This method returns the SSH username configured in the provided configuration object.
     *
     * @param config The configuration object containing the provider-specific settings
     * @return The SSH username configured in the configuration object, or "root" if not specified
     */
    public static String getSSHUser(ObjectConfig<VPSProvider> config) {
        return config.config().getOrDefault(Constants.SSH_USER_VAR, "root");
    }
}
