package com.fathzer.odvpn;

import com.fathzer.odvpn.utils.Registerable;

/**
 * Abstract provider.
 * Providers are responsible for a part of the infrastructure (VPS, DDNS, maybe something else in the future).
 */
public abstract class Provider<T> {
    protected T settings;

    /**
     * Gets the ID of the provider.
     * @return the provider ID
     */
    public String id() {
        Registerable registerable = getClass().getAnnotation(Registerable.class);
        if (registerable == null) {
            throw new IllegalArgumentException("Provider " + getClass().getName() + " is not registered");
        }
        return registerable.value();
    }

    /**
     * Gets the name of the provider.
     * @return the provider name
     */
    public abstract String name();

    /**
     * Gets the configuration for the VPS provider.
     * @return The configuration object containing the provider-specific settings
     */
    public T getSettings() {
        return settings;
    }

    /**
     * Sets the configuration for the VPS provider.
     * @param settings The configuration object containing the provider-specific settings
     */
    public void setSettings(T settings) {
        this.settings = settings;
    }

    /** Gets the class of the configuration object. */
    public abstract Class<T> getConfigClass();

    /**
     * Resolves a value that may contain variables referenced as `${variable}`.
     * @param value the value to resolve
     * @return the resolved value
     */
    public String resolve(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("${") && value.endsWith("}")) {
            final String variable = value.substring(2, value.length() - 1);
            final String resolved = getValue(variable);
            if (resolved != null) {
                return resolved;
            }
        }
        return value;
    }

    /**
     * Gets the value of a variable.
     * <br>The default implementation returns the value of the environment variable with the given name.
     * @param variable the variable to get the value of
     * @return the value of the variable, or null if the variable is not defined
     */
    protected String getValue(String variable) {
        return System.getenv(variable);
    }
}
