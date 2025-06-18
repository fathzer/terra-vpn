package com.fathzer.terravpn;

import java.util.Optional;

/**
 * Interface for providers.
 * Providers are responsible for a part of the infrastructure (VPS, DDNS, maybe something else in the future).
 */
public interface Provider {
    /**
     * Gets the unique identifier of the provider.
     * @return the provider identifier.
     */
    String id();

    /**
     * Gets the name of the provider.
     * @return the provider name
     */
    default String name() {
        return id();
    }

    /**
     * Gets the description of the provider.
     * @return the provider description
     */
    default Optional<String> description() {
        return Optional.empty();
    }
}
