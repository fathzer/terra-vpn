package com.fathzer.terravpn;

import com.fathzer.terravpn.utils.Registerable;

/**
 * Interface for providers.
 * Providers are responsible for a part of the infrastructure (VPS, DDNS, maybe something else in the future).
 */
public interface Provider {
    /**
     * Gets the ID of the provider.
     * @return the provider ID
     */
    default String id() {
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
    String name();
}
