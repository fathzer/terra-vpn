package com.fathzer.odvpn;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public abstract class VPSProvider<T> extends Provider<T> {
    /**
     * Represents the state of a Virtual Private Server (VPS) instance.
     */
    public enum Status {
        /**
         * The VPS instance is starting.
         */
        STARTING,
        /**
         * The VPS instance is not ready to be used but have an IP address.
         */
        IP_READY,
        /**
         * The VPS instance is ready to be used.
         */
        READY,
        /**
         * The VPS instance is being deleted.
         */
        STOPPED
    }

    /**
     * Represents the state of a Virtual Private Server (VPS) instance.
     * This record holds essential identifying information about a VPS.
     * @param id The unique identifier of the VPS instance in the provider's system
     * @param ip The public IP address assigned to the VPS instance (could be null, or not, if server is not ready)
     * @param status The state of the VPS instance
     */
    public record VPSState(String id, String ip, Status status) {
        public VPSState {
            if (status == null) {
                throw new IllegalArgumentException("status cannot be null");
            }
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("id cannot be null or empty");
            }
            if ((status == Status.READY || status == Status.IP_READY) && ip == null) {
                throw new IllegalArgumentException("ip cannot be null when state is " + status.name());
            }
            if (ip != null && !ip.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$")) {
                throw new IllegalArgumentException("ip must be a valid IPv4 address");
            }
        }
    }

    /**
     * Checks the settings for the VPS provider.
     * This method is used to validate the configuration before creating a VPS instance.
     *
     * @return A list of configuration errors, or an empty list if the configuration is valid
     */
    public abstract List<String> checkConfiguration() throws IOException;

    /**
     * Checks if the VPS instance exists.
     * @param id The unique identifier of the VPS instance in the provider's system
     * @return true if the VPS instance exists, false otherwise
     */
    public abstract boolean exists(String id) throws IOException;

    /**
     * Creates a new Virtual Private Server (VPS) instance.
     * This method is responsible for provisioning a new VPS instance based on the provided parameters.
     *
     * @param progress A consumer that receives progress updates during the VPS creation process
     * @return A VPSState object representing the created VPS instance, containing its unique identifier and public IP address
     */
    public abstract VPSState createVPS(Consumer<VPSState> progress) throws IOException;

    /**
     * Creates a human-readable name for the VPS instance.
     * @return By default, returns a name based on the current date and time prefixed with "odvpn-".
     */
    protected String getInstanceName() {
        return "odvpn-" + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm'GMT'x"));
    }
    /**
     * Deletes an existing Virtual Private Server (VPS) instance.
     * This method is responsible for removing a VPS instance from the provider's system.
     * @param id The unique identifier of the VPS instance to be deleted
     */
    public abstract void deleteVPS(String id) throws IOException;

    /**
     * Gets the SSH username for the VPS provider.
     * <br>The default implementation returns "root".
     * @return The SSH username to use to connect to the VPS instance
     */
    public String getSSHUser() {
        return "root";
    }
}
